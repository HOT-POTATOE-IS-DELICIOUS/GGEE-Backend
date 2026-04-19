package team.hotpotato.domain.reaction.infrastructure.comment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

@RequiredArgsConstructor
public class CommentDeduplicationProcessorSupplier implements FixedKeyProcessorSupplier<String, DeduplicatedCommentMessage, DeduplicatedCommentMessage> {

    private final String storeName;
    private final Duration ttl;
    private final Duration cleanupInterval;

    @Override
    public FixedKeyProcessor<String, DeduplicatedCommentMessage, DeduplicatedCommentMessage> get() {
        return new CommentDeduplicationProcessor(storeName, ttl, cleanupInterval);
    }

    private static final class CommentDeduplicationProcessor
            implements FixedKeyProcessor<String, DeduplicatedCommentMessage, DeduplicatedCommentMessage> {

        private final String storeName;
        private final long ttlMs;
        private final Duration cleanupInterval;

        private FixedKeyProcessorContext<String, DeduplicatedCommentMessage> context;
        private KeyValueStore<String, Long> dedupStore;

        private CommentDeduplicationProcessor(String storeName, Duration ttl, Duration cleanupInterval) {
            this.storeName = storeName;
            this.ttlMs = ttl.toMillis();
            this.cleanupInterval = cleanupInterval;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void init(FixedKeyProcessorContext<String, DeduplicatedCommentMessage> context) {
            this.context = context;
            this.dedupStore = context.getStateStore(storeName);
            context.schedule(cleanupInterval, PunctuationType.WALL_CLOCK_TIME, this::purgeExpiredEntries);
        }

        @Override
        public void process(FixedKeyRecord<String, DeduplicatedCommentMessage> record) {
            if (record == null || record.key() == null || record.value() == null) return;

            long eventTimestamp = record.value().eventTimestampMs() != null
                    ? record.value().eventTimestampMs()
                    : record.timestamp();

            long cutoffTimestamp = eventTimestamp - ttlMs;
            Long previousSeenTimestamp = dedupStore.get(record.key());

            // 슬라이딩 TTL: 중복이 들어올 때마다 타임스탬프를 갱신한다.
            // 같은 댓글이 계속 크롤링되는 동안에는 억제되고,
            // TTL 기간 동안 크롤링이 없다가 다시 나타나면 재방출된다.
            dedupStore.put(record.key(), eventTimestamp);

            if (previousSeenTimestamp != null && previousSeenTimestamp >= cutoffTimestamp) {
                return;
            }

            context.forward(record.withTimestamp(eventTimestamp));
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
    }
}
