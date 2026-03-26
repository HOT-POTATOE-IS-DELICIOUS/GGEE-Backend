package team.hotpotato.infrastructure.perf;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.community.CrawlCommentMessage;
import team.hotpotato.domain.reaction.application.community.CrawlPostMessage;
import team.hotpotato.domain.reaction.application.community.CrawlResultMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/perf")
@RequiredArgsConstructor
public class PerfController {

    private final DatabaseClient db;

    // Shared dedup state: simulates Kafka Streams TTL state store across requests
    // key = commentId|postUrl, value = lastSeenEpochMs
    private final ConcurrentHashMap<String, Long> dedupStore = new ConcurrentHashMap<>();
    private static final long TTL_MS = 86_400_000L; // 24h

    /**
     * 개선 전: 모든 댓글에 대해 SELECT → INSERT or UPDATE
     */
    @PostMapping("/before")
    public Mono<PerfResult> before(@RequestBody CrawlResultMessage message) {
        long start = System.currentTimeMillis();
        List<CrawlCommentMessage> allComments = flattenComments(message);

        return Flux.fromIterable(allComments)
                .concatMap(comment -> {
                    String key = comment.id() + "|" + getPostUrl(message, comment);
                    // ① SELECT
                    return db.sql("SELECT 1 FROM perf_comments WHERE comment_key = :key")
                            .bind("key", key)
                            .fetch().one()
                            .flatMap(row ->
                                    // ② UPDATE
                                    db.sql("""
                                            UPDATE perf_comments
                                            SET likes = :likes, dislikes = :dislikes, crawled_at = :crawledAt
                                            WHERE comment_key = :key
                                            """)
                                            .bind("likes", comment.likes() != null ? comment.likes() : "0")
                                            .bind("dislikes", comment.dislikes() != null ? comment.dislikes() : "0")
                                            .bind("crawledAt", message.timestamp())
                                            .bind("key", key)
                                            .fetch().rowsUpdated()
                                            .thenReturn("update")
                            )
                            .switchIfEmpty(
                                    // ② INSERT (not exists)
                                    db.sql("""
                                            INSERT INTO perf_comments
                                                (comment_key, site, keyword, post_url, comment_id, author, content, likes, dislikes, crawled_at)
                                            VALUES (:key, :site, :keyword, :postUrl, :commentId, :author, :content, :likes, :dislikes, :crawledAt)
                                            """)
                                            .bind("key", key)
                                            .bind("site", message.site())
                                            .bind("keyword", message.keyword())
                                            .bind("postUrl", getPostUrl(message, comment))
                                            .bind("commentId", comment.id())
                                            .bind("author", comment.author() != null ? comment.author() : "")
                                            .bind("content", comment.content() != null ? comment.content() : "")
                                            .bind("likes", comment.likes() != null ? comment.likes() : "0")
                                            .bind("dislikes", comment.dislikes() != null ? comment.dislikes() : "0")
                                            .bind("crawledAt", message.timestamp())
                                            .fetch().rowsUpdated()
                                            .thenReturn("insert")
                            );
                })
                .collectList()
                .map(ops -> {
                    long elapsed = System.currentTimeMillis() - start;
                    long updates = ops.stream().filter("update"::equals).count();
                    long inserts = ops.stream().filter("insert"::equals).count();
                    return new PerfResult(
                            allComments.size(),
                            allComments.size(),                             // all comments → DB
                            (int) (allComments.size() + updates + inserts), // SELECT×N + (INSERT or UPDATE)×N
                            0,
                            elapsed
                    );
                });
    }

    /**
     * 개선 후: TTL 기반 중복 제거 → UPSERT (INSERT ON CONFLICT)
     */
    @PostMapping("/after")
    public Mono<PerfResult> after(@RequestBody CrawlResultMessage message) {
        long start = System.currentTimeMillis();
        List<CrawlCommentMessage> allComments = flattenComments(message);

        long now = Instant.now().toEpochMilli();
        long cutoff = now - TTL_MS;

        // Dedup in-memory (simulates Kafka Streams RocksDB state store)
        List<Map.Entry<String, CrawlCommentMessage>> unique = new ArrayList<>();
        for (CrawlCommentMessage c : allComments) {
            String key = c.id() + "|" + getPostUrl(message, c);
            Long prev = dedupStore.get(key);
            dedupStore.put(key, now);
            if (prev == null || prev < cutoff) {
                unique.add(Map.entry(key, c));
            }
        }

        int filtered = allComments.size() - unique.size();

        if (unique.isEmpty()) {
            return Mono.just(new PerfResult(allComments.size(), 0, 0, filtered, System.currentTimeMillis() - start));
        }

        return Flux.fromIterable(unique)
                .concatMap(entry -> {
                    String key = entry.getKey();
                    CrawlCommentMessage c = entry.getValue();
                    return db.sql("""
                            INSERT INTO perf_comments
                                (comment_key, site, keyword, post_url, comment_id, author, content, likes, dislikes, crawled_at)
                            VALUES (:key, :site, :keyword, :postUrl, :commentId, :author, :content, :likes, :dislikes, :crawledAt)
                            ON CONFLICT (comment_key) DO UPDATE
                                SET likes      = EXCLUDED.likes,
                                    dislikes   = EXCLUDED.dislikes,
                                    crawled_at = EXCLUDED.crawled_at
                            """)
                            .bind("key", key)
                            .bind("site", message.site())
                            .bind("keyword", message.keyword())
                            .bind("postUrl", getPostUrl(message, c))
                            .bind("commentId", c.id())
                            .bind("author", c.author() != null ? c.author() : "")
                            .bind("content", c.content() != null ? c.content() : "")
                            .bind("likes", c.likes() != null ? c.likes() : "0")
                            .bind("dislikes", c.dislikes() != null ? c.dislikes() : "0")
                            .bind("crawledAt", message.timestamp())
                            .fetch().rowsUpdated();
                })
                .then(Mono.fromCallable(() -> {
                    long elapsed = System.currentTimeMillis() - start;
                    return new PerfResult(allComments.size(), unique.size(), unique.size(), filtered, elapsed);
                }));
    }

    /** 성능 테스트 전 상태 초기화 */
    @DeleteMapping("/reset")
    public Mono<Void> reset() {
        dedupStore.clear();
        return db.sql("TRUNCATE perf_comments").fetch().rowsUpdated().then();
    }

    // ─── helpers ───

    private List<CrawlCommentMessage> flattenComments(CrawlResultMessage msg) {
        if (msg.results() == null) return List.of();
        return msg.results().stream()
                .filter(p -> p.comments() != null)
                .flatMap(p -> p.comments().stream())
                .toList();
    }

    private String getPostUrl(CrawlResultMessage msg, CrawlCommentMessage comment) {
        if (msg.results() == null) return "";
        return msg.results().stream()
                .filter(p -> p.comments() != null && p.comments().contains(comment))
                .map(CrawlPostMessage::url)
                .findFirst()
                .orElse("");
    }

    public record PerfResult(
            int totalComments,
            int dbWrites,
            int dbQueries,
            int filtered,
            long elapsedMs
    ) {}
}
