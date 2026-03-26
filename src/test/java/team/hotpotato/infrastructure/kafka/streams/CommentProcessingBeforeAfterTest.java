package team.hotpotato.infrastructure.kafka.streams;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team.hotpotato.domain.reaction.application.community.*;
import team.hotpotato.infrastructure.kafka.JsonSerdeFactory;
import team.hotpotato.infrastructure.kafka.community.CommentDedupStreamConfiguration;
import team.hotpotato.infrastructure.kafka.community.CrawlerStreamsProperties;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 실제 PostgreSQL(Docker)에 연결해 개선 전/후 성능을 비교하는 테스트.
 *
 * [개선 전] 모든 댓글에 대해 SELECT → INSERT or UPDATE (2-query 패턴)
 * [개선 후] Kafka Streams TTL 중복 제거 → 통과 댓글만 INSERT ON CONFLICT (UPSERT)
 *
 * 실행 전 준비:
 *   docker compose up postgres -d --wait
 */
class CommentProcessingBeforeAfterTest {

    // ==== DB 연결 정보 (.env 기준) ====
    private static final String JDBC_URL  = "jdbc:postgresql://localhost:5432/ggee";
    private static final String DB_USER   = "ggee-user";
    private static final String DB_PASS   = "sj32993329&";

    // ==== 시나리오 파라미터 ====
    private static final int KEYWORDS      = 10;
    private static final int POSTS         = 5;
    private static final int COMMENTS      = 40;
    private static final int RECRAWL_COUNT = 8;   // TTL 이내 재크롤 횟수

    private static final JsonSerdeFactory SERDE_FACTORY = new JsonSerdeFactory(
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    );

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS perf_comments");
            st.execute("""
                    CREATE TABLE perf_comments (
                        comment_key  VARCHAR(512) PRIMARY KEY,
                        site         VARCHAR(64)  NOT NULL,
                        keyword      VARCHAR(128) NOT NULL,
                        post_url     VARCHAR(512) NOT NULL,
                        comment_id   INT          NOT NULL,
                        author       VARCHAR(128),
                        content      TEXT,
                        likes        VARCHAR(16),
                        dislikes     VARCHAR(16),
                        crawled_at   VARCHAR(64)
                    )
                    """);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS perf_comments");
        }
        conn.close();
    }

    @Test
    void compareBeforeAndAfter() throws Exception {
        List<CrawlResultMessage> allCrawlResults = buildCrawlResults();
        int totalComments = countComments(allCrawlResults);

        // ─── 개선 전: SELECT + INSERT/UPDATE ───
        resetTable();
        long beforeStart = System.nanoTime();
        BeforeResult before = runBefore(allCrawlResults);
        long beforeDbMs = (System.nanoTime() - beforeStart) / 1_000_000;

        // ─── Kafka Streams 중복 제거 (always-on 서비스, 비동기 처리) ───
        long streamStart = System.nanoTime();
        List<DeduplicatedCommentMessage> deduped = runDedupStream(allCrawlResults);
        long streamMs = (System.nanoTime() - streamStart) / 1_000_000;

        // ─── 개선 후: UPSERT만 ───
        resetTable();
        long afterStart = System.nanoTime();
        AfterResult after = runAfter(deduped);
        long afterDbMs = (System.nanoTime() - afterStart) / 1_000_000;

        // ─── 결과 출력 ───
        printResult(totalComments, allCrawlResults.size(), before, beforeDbMs,
                after, streamMs, afterDbMs);
    }

    // =====================================================================
    //  개선 전: 모든 댓글에 대해 SELECT → INSERT or UPDATE
    // =====================================================================

    private BeforeResult runBefore(List<CrawlResultMessage> crawlResults) throws SQLException {
        int selectCount = 0, insertCount = 0, updateCount = 0;

        String selectSql = "SELECT 1 FROM perf_comments WHERE comment_key = ?";
        String insertSql = """
                INSERT INTO perf_comments
                    (comment_key, site, keyword, post_url, comment_id, author, content, likes, dislikes, crawled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String updateSql = """
                UPDATE perf_comments
                SET likes = ?, dislikes = ?, crawled_at = ?
                WHERE comment_key = ?
                """;

        try (PreparedStatement sel = conn.prepareStatement(selectSql);
             PreparedStatement ins = conn.prepareStatement(insertSql);
             PreparedStatement upd = conn.prepareStatement(updateSql)) {

            for (CrawlResultMessage msg : crawlResults) {
                for (CrawlPostMessage post : msg.results()) {
                    for (CrawlCommentMessage c : post.comments()) {
                        String key = c.id() + "|" + post.url();

                        // ① SELECT
                        sel.setString(1, key);
                        boolean exists;
                        try (ResultSet rs = sel.executeQuery()) {
                            exists = rs.next();
                        }
                        selectCount++;

                        if (exists) {
                            // ② UPDATE
                            upd.setString(1, c.likes());
                            upd.setString(2, c.dislikes());
                            upd.setString(3, msg.timestamp());
                            upd.setString(4, key);
                            upd.executeUpdate();
                            updateCount++;
                        } else {
                            // ② INSERT
                            ins.setString(1, key);
                            ins.setString(2, msg.site());
                            ins.setString(3, msg.keyword());
                            ins.setString(4, post.url());
                            ins.setInt(5, c.id());
                            ins.setString(6, c.author());
                            ins.setString(7, c.content());
                            ins.setString(8, c.likes());
                            ins.setString(9, c.dislikes());
                            ins.setString(10, msg.timestamp());
                            ins.executeUpdate();
                            insertCount++;
                        }
                    }
                }
            }
        }

        return new BeforeResult(selectCount, insertCount, updateCount,
                selectCount + insertCount + updateCount);
    }

    // =====================================================================
    //  개선 후: Kafka Streams 중복 제거 → INSERT ON CONFLICT (UPSERT)
    // =====================================================================

    private List<DeduplicatedCommentMessage> runDedupStream(List<CrawlResultMessage> crawlResults) {
        CrawlerStreamsProperties props = new CrawlerStreamsProperties(
                "crawl.result", "crawl.comment.deduped",
                "crawler-comment-dedup-store",
                Duration.ofDays(1), Duration.ofMinutes(5)
        );

        StreamsBuilder builder = new StreamsBuilder();
        new CommentDedupStreamConfiguration().commentDedupStream(builder, props, SERDE_FACTORY);

        Properties streamProps = new Properties();
        streamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "before-after-pg-test");
        streamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        streamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.Serdes$StringSerde");

        try (TopologyTestDriver driver = new TopologyTestDriver(builder.build(), streamProps)) {
            TestInputTopic<String, CrawlResultMessage> in = driver.createInputTopic(
                    "crawl.result", new StringSerializer(),
                    SERDE_FACTORY.serde(CrawlResultMessage.class).serializer());
            TestOutputTopic<String, DeduplicatedCommentMessage> out = driver.createOutputTopic(
                    "crawl.comment.deduped", new StringDeserializer(),
                    SERDE_FACTORY.serde(DeduplicatedCommentMessage.class).deserializer());

            Instant base = Instant.parse("2026-01-01T00:00:00Z");
            int idx = 0;
            for (CrawlResultMessage msg : crawlResults) {
                Instant ts = base.plusSeconds(idx++ * 1800L);  // 30분 간격 (24h TTL 이내)
                in.pipeInput(msg.jobId(), msg, ts);
            }
            return out.readValuesToList();
        }
    }

    private AfterResult runAfter(List<DeduplicatedCommentMessage> deduped) throws SQLException {
        // PostgreSQL UPSERT: INSERT ... ON CONFLICT DO UPDATE (SELECT 없음)
        String upsertSql = """
                INSERT INTO perf_comments
                    (comment_key, site, keyword, post_url, comment_id, author, content, likes, dislikes, crawled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (comment_key) DO UPDATE
                    SET likes      = EXCLUDED.likes,
                        dislikes   = EXCLUDED.dislikes,
                        crawled_at = EXCLUDED.crawled_at
                """;

        int upsertCount = 0;
        try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
            for (DeduplicatedCommentMessage d : deduped) {
                ps.setString(1, d.id());
                ps.setString(2, d.site());
                ps.setString(3, d.keyword());
                ps.setString(4, d.postUrl());
                ps.setInt(5, d.commentId());
                ps.setString(6, d.author());
                ps.setString(7, d.content());
                ps.setString(8, d.likes());
                ps.setString(9, d.dislikes());
                ps.setString(10, d.crawledAt());
                ps.executeUpdate();
                upsertCount++;
            }
        }
        return new AfterResult(deduped.size(), upsertCount);
    }

    // =====================================================================
    //  데이터 빌더 / 유틸
    // =====================================================================

    private List<CrawlResultMessage> buildCrawlResults() {
        List<CrawlResultMessage> results = new ArrayList<>();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        int jobSeq = 0;

        for (int k = 0; k < KEYWORDS; k++) {
            for (int r = 0; r < RECRAWL_COUNT; r++) {
                String ts = base.plusSeconds(k * 3600 + r * 1800).toString();
                List<CrawlPostMessage> posts = new ArrayList<>();

                for (int p = 0; p < POSTS; p++) {
                    List<CrawlCommentMessage> comments = new ArrayList<>();
                    for (int c = 0; c < COMMENTS; c++) {
                        comments.add(new CrawlCommentMessage(
                                p * 10000 + c, null, "user-" + c, ts,
                                "content-" + c,
                                String.valueOf(c % 200 + r),  // likes 는 재크롤마다 약간 변동
                                "0"));
                    }
                    posts.add(new CrawlPostMessage(
                            "Post " + p, "1", "10", "2", ts, "body", comments,
                            "https://community.example/keyword-" + k + "/post-" + p));
                }

                results.add(new CrawlResultMessage(
                        "job-" + (jobSeq++), ts, "completed",
                        "theqoo", "keyword-" + k, POSTS, POSTS, POSTS, 0, posts));
            }
        }
        return results;
    }

    private int countComments(List<CrawlResultMessage> all) {
        return all.stream()
                .mapToInt(m -> m.results().stream().mapToInt(p -> p.comments().size()).sum())
                .sum();
    }

    private void resetTable() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("TRUNCATE perf_comments");
        }
    }

    // =====================================================================
    //  결과 출력
    // =====================================================================

    private void printResult(int totalComments, int crawlEvents,
                             BeforeResult before, long beforeDbMs,
                             AfterResult after, long streamMs, long afterDbMs) {

        double filterRate   = 100.0 * (totalComments - after.dedupedCount) / totalComments;
        double queryReduce  = 100.0 * (before.totalDbOps - after.upsertCount) / before.totalDbOps;
        double dbSpeedup    = (double) beforeDbMs / Math.max(afterDbMs, 1);

        System.out.println("\n" + "=".repeat(65));
        System.out.println("  개선 전/후 실제 DB 쿼리 비교 (PostgreSQL 17, Docker)");
        System.out.println("=".repeat(65));
        System.out.printf("""
                테스트 조건
                  데이터베이스      : PostgreSQL 17 (Docker, localhost:5432)
                  키워드 수         : %,d 개
                  게시글 수/키워드  : %,d 개
                  댓글 수/게시글    : %,d 개
                  재크롤 횟수       : %,d 회  (30분 간격, 24h TTL 이내)
                  총 크롤링 이벤트  : %,d 건
                  총 입력 댓글 수   : %,d 건
                %n""", KEYWORDS, POSTS, COMMENTS, RECRAWL_COUNT, crawlEvents, totalComments);

        System.out.println("[ 개선 전 ] SELECT + INSERT/UPDATE 패턴");
        System.out.printf("  SELECT 쿼리         : %,6d 건%n", before.selectCount);
        System.out.printf("  INSERT 쿼리         : %,6d 건%n", before.insertCount);
        System.out.printf("  UPDATE 쿼리         : %,6d 건%n", before.updateCount);
        System.out.printf("  총 DB 쿼리 수       : %,6d 건%n", before.totalDbOps);
        System.out.printf("  DB 실행 시간        : %d ms%n", beforeDbMs);
        System.out.printf("  쿼리당 평균         : %.2f ms%n", (double) beforeDbMs / before.totalDbOps);
        System.out.println();

        System.out.println("[ 개선 후 ] Kafka Streams 중복 제거 + UPSERT 패턴");
        System.out.printf("  Streams 처리 시간   : %d ms  (always-on 서비스, 비동기)%n", streamMs);
        System.out.printf("  Streams 통과 댓글   : %,6d 건  (중복 필터링률 %.1f%%)%n",
                after.dedupedCount, filterRate);
        System.out.printf("  UPSERT 쿼리         : %,6d 건  (SELECT 없음)%n", after.upsertCount);
        System.out.printf("  DB 실행 시간        : %d ms%n", afterDbMs);
        System.out.printf("  쿼리당 평균         : %.2f ms%n",
                after.upsertCount > 0 ? (double) afterDbMs / after.upsertCount : 0);
        System.out.println();

        System.out.println("[ 결과 ]");
        System.out.printf("  DB 쿼리 수 감소     : %.1f%%  (%,d → %,d 건)%n",
                queryReduce, before.totalDbOps, after.upsertCount);
        System.out.printf("  DB 실행 시간 단축   : %.1fx  (%d ms → %d ms)%n",
                dbSpeedup, beforeDbMs, afterDbMs);
        System.out.println("=".repeat(65));
    }

    // =====================================================================
    //  내부 결과 타입
    // =====================================================================

    record BeforeResult(int selectCount, int insertCount, int updateCount, int totalDbOps) {}
    record AfterResult(int dedupedCount, int upsertCount) {}
}
