package team.hotpotato.domain.reaction.infrastructure.comment;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.infrastructure.crawler.message.CrawlCommentMessage;
import team.hotpotato.infrastructure.crawler.message.CrawlPostMessage;
import team.hotpotato.infrastructure.crawler.message.CrawlResultMessage;

@RequiredArgsConstructor
public class CommentDeduplicationProcessorSupplier implements ProcessorSupplier<String, CrawlResultMessage, String, CommentDedupResult> {

    private final String storeName;
    private final Duration ttl;
    private final Duration cleanupInterval;
    private final IdGenerator idGenerator;

    @Override
    public Processor<String, CrawlResultMessage, String, CommentDedupResult> get() {
        return new CommentDeduplicationProcessor(storeName, ttl, cleanupInterval, idGenerator);
    }

    private static final class CommentDeduplicationProcessor
            implements Processor<String, CrawlResultMessage, String, CommentDedupResult> {

        private final String storeName;
        private final long ttlMs;
        private final Duration cleanupInterval;
        private final IdGenerator idGenerator;

        private ProcessorContext<String, CommentDedupResult> context;
        private KeyValueStore<String, Long> dedupStore;

        private CommentDeduplicationProcessor(String storeName, Duration ttl, Duration cleanupInterval, IdGenerator idGenerator) {
            this.storeName = storeName;
            this.ttlMs = ttl.toMillis();
            this.cleanupInterval = cleanupInterval;
            this.idGenerator = idGenerator;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void init(ProcessorContext<String, CommentDedupResult> context) {
            this.context = context;
            this.dedupStore = context.getStateStore(storeName);
            context.schedule(cleanupInterval, PunctuationType.WALL_CLOCK_TIME, this::purgeExpiredEntries);
        }

        @Override
        public void process(Record<String, CrawlResultMessage> record) {
            CrawlResultMessage crawlResult = record.value();
            if (crawlResult == null || crawlResult.results() == null) return;

            long eventTimestampMs = parseTimestamp(crawlResult.timestamp());

            for (CrawlPostMessage post : crawlResult.results()) {
                if (post == null || isBlank(post.url()) || post.comments() == null || post.comments().isEmpty()) {
                    continue;
                }

                String postUrl = post.url().trim();
                List<CrawlCommentMessage> newComments = filterNewComments(post.comments(), postUrl, eventTimestampMs);

                if (!newComments.isEmpty()) {
                    long postId = idGenerator.generateId();
                    context.forward(new Record<>(
                            postUrl,
                            new CommentDedupResult(
                                    postId,
                                    crawlResult.site(),
                                    crawlResult.keyword(),
                                    OffsetDateTime.parse(crawlResult.timestamp()),
                                    eventTimestampMs,
                                    postUrl,
                                    post.title(),
                                    newComments
                            ),
                            eventTimestampMs
                    ));
                }
            }
        }

        private List<CrawlCommentMessage> filterNewComments(List<CrawlCommentMessage> comments, String postUrl, long eventTimestampMs) {
            long cutoff = eventTimestampMs - ttlMs;
            List<CrawlCommentMessage> newComments = new ArrayList<>();

            for (CrawlCommentMessage comment : comments) {
                if (comment == null || comment.id() == null) continue;

                String dedupKey = comment.id() + "|" + postUrl;
                Long previousSeenAt = dedupStore.get(dedupKey);

                // 슬라이딩 TTL: 중복이 들어올 때마다 타임스탬프를 갱신한다.
                dedupStore.put(dedupKey, eventTimestampMs);

                if (previousSeenAt == null || previousSeenAt < cutoff) {
                    newComments.add(comment);
                }
            }

            return newComments;
        }

        @Override
        public void close() {
        }

        private void purgeExpiredEntries(long now) {
            long expirationThreshold = now - ttlMs;
            List<String> expiredKeys = new ArrayList<>();

            try (KeyValueIterator<String, Long> iterator = dedupStore.all()) {
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if (entry.value == null || entry.value < expirationThreshold) {
                        expiredKeys.add(entry.key);
                    }
                }
            }

            for (String expiredKey : expiredKeys) {
                dedupStore.delete(expiredKey);
            }
        }

        private static long parseTimestamp(String timestamp) {
            if (timestamp == null || timestamp.isBlank()) return System.currentTimeMillis();
            try {
                return Instant.parse(timestamp.trim()).toEpochMilli();
            } catch (Exception ignored) {
                return System.currentTimeMillis();
            }
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
