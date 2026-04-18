package team.hotpotato.infrastructure.event.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.test.StepVerifier;
import team.hotpotato.domain.reaction.application.community.CrawlJobCreateMessage;
import team.hotpotato.infrastructure.kafka.EventTopics;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("크롤링 작업 생성 발행 어댑터 단위 테스트")
class CrawlJobEventPublisherTest {

	@Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private CrawlJobEventPublisher crawlJobEventPublisher;

    @BeforeEach
    void setUp() {
        crawlJobEventPublisher = new CrawlJobEventPublisher(
                kafkaTemplate,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("크롤링 작업 생성 메시지를 지정 토픽으로 발행한다")
    void publishSendsMessageToConfiguredTopic() {
        CrawlJobCreateMessage message = new CrawlJobCreateMessage(
                "req-1",
                "키워드",
                3
        );
        when(kafkaTemplate.send(
                eq(EventTopics.CRAWL_JOB_CREATE),
                eq("req-1"),
                eq("{\"job_id\":\"req-1\",\"keyword\":\"키워드\",\"max_pages\":3}")
        )).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(crawlJobEventPublisher.publish(message))
                .verifyComplete();

        verify(kafkaTemplate).send(
                EventTopics.CRAWL_JOB_CREATE,
                "req-1",
                "{\"job_id\":\"req-1\",\"keyword\":\"키워드\",\"max_pages\":3}"
        );
    }
}
