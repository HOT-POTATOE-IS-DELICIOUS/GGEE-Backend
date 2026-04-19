package team.hotpotato.domain.reaction.infrastructure.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.infrastructure.crawler.CrawlerTopics;
import team.hotpotato.infrastructure.crawler.message.CrawlCommentMessage;
import team.hotpotato.infrastructure.crawler.message.CrawlPostMessage;
import team.hotpotato.infrastructure.crawler.message.CrawlResultMessage;
import team.hotpotato.infrastructure.kafka.config.JsonSerdeFactory;

class CommentDedupStreamConfigurationTest {

    private static final long FIXED_POST_ID = 123456789L;
    private static final IdGenerator FIXED_ID_GENERATOR = () -> FIXED_POST_ID;

    @Test
    void suppressesDuplicateCommentsWithinOneDayTtl() {
        try (TopologyTestDriver driver = createDriver()) {
            TestInputTopic<String, CrawlResultMessage> inputTopic = inputTopic(driver);
            TestOutputTopic<String, DeduplicatedPostMessage> postOutputTopic = postOutputTopic(driver);
            TestOutputTopic<String, DeduplicatedCommentMessage> commentOutputTopic = commentOutputTopic(driver);

            String url = "https://community.example/posts/1";
            inputTopic.pipeInput("job-1", crawlResult("job-1", url, 1001, "2026-03-23T00:00:00Z"), Instant.parse("2026-03-23T00:00:00Z"));
            inputTopic.pipeInput("job-2", crawlResult("job-2", url, 1001, "2026-03-23T06:00:00Z"), Instant.parse("2026-03-23T06:00:00Z"));

            List<DeduplicatedPostMessage> posts = postOutputTopic.readValuesToList();
            List<DeduplicatedCommentMessage> comments = commentOutputTopic.readValuesToList();

            assertThat(posts).hasSize(1);
            assertThat(posts.getFirst().postUrl()).isEqualTo(url);
            assertThat(posts.getFirst().postId()).isEqualTo(FIXED_POST_ID);

            assertThat(comments).hasSize(1);
            assertThat(comments.getFirst().postId()).isEqualTo(FIXED_POST_ID);
            assertThat(comments.getFirst().id()).isEqualTo(1001);
        }
    }

    @Test
    void reEmitsCommentAfterTtlExpires() {
        try (TopologyTestDriver driver = createDriver()) {
            TestInputTopic<String, CrawlResultMessage> inputTopic = inputTopic(driver);
            TestOutputTopic<String, DeduplicatedPostMessage> postOutputTopic = postOutputTopic(driver);
            TestOutputTopic<String, DeduplicatedCommentMessage> commentOutputTopic = commentOutputTopic(driver);

            String url = "https://community.example/posts/2";
            inputTopic.pipeInput("job-1", crawlResult("job-1", url, 2002, "2026-03-23T00:00:00Z"), Instant.parse("2026-03-23T00:00:00Z"));
            inputTopic.pipeInput("job-2", crawlResult("job-2", url, 2002, "2026-03-24T01:00:01Z"), Instant.parse("2026-03-24T01:00:01Z"));

            assertThat(postOutputTopic.readValuesToList()).hasSize(2);
            assertThat(commentOutputTopic.readValuesToList()).hasSize(2);
        }
    }

    private static final JsonSerdeFactory SERDE_FACTORY = new JsonSerdeFactory(
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    );

    private TopologyTestDriver createDriver() {
        CommentDedupStreamProperties properties = new CommentDedupStreamProperties(
                "crawler-comment-dedup-store",
                Duration.ofDays(1),
                Duration.ofMinutes(5)
        );

        CommentDedupStreamConfiguration configuration = new CommentDedupStreamConfiguration();
        StreamsBuilder builder = new StreamsBuilder();
        configuration.commentDedupStream(builder, properties, SERDE_FACTORY, FIXED_ID_GENERATOR);

        Properties streamProperties = new Properties();
        streamProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "comment-dedup-test");
        streamProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        streamProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");

        return new TopologyTestDriver(builder.build(), streamProperties);
    }

    private TestInputTopic<String, CrawlResultMessage> inputTopic(TopologyTestDriver driver) {
        return driver.createInputTopic(
                CrawlerTopics.CRAWL_RESULT,
                new StringSerializer(),
                SERDE_FACTORY.serde(CrawlResultMessage.class).serializer()
        );
    }

    private TestOutputTopic<String, DeduplicatedPostMessage> postOutputTopic(TopologyTestDriver driver) {
        return driver.createOutputTopic(
                CrawlerTopics.CRAWL_POST_DEDUPED,
                new StringDeserializer(),
                SERDE_FACTORY.serde(DeduplicatedPostMessage.class).deserializer()
        );
    }

    private TestOutputTopic<String, DeduplicatedCommentMessage> commentOutputTopic(TopologyTestDriver driver) {
        return driver.createOutputTopic(
                CrawlerTopics.CRAWL_COMMENT_DEDUPED,
                new StringDeserializer(),
                SERDE_FACTORY.serde(DeduplicatedCommentMessage.class).deserializer()
        );
    }

    private CrawlResultMessage crawlResult(String jobId, String url, int commentId, String crawledAt) {
        CrawlCommentMessage comment = new CrawlCommentMessage(
                commentId,
                null,
                "alice",
                crawledAt,
                "first comment",
                "3",
                "0"
        );

        CrawlPostMessage post = new CrawlPostMessage(
                "sample post",
                "1",
                "10",
                "2",
                crawledAt,
                "body",
                List.of(comment),
                url
        );

        return new CrawlResultMessage(
                jobId,
                crawledAt,
                "completed",
                "theqoo",
                "keyword",
                1,
                1,
                1,
                0,
                List.of(post)
        );
    }
}
