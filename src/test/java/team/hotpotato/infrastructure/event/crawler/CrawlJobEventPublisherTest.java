package team.hotpotato.infrastructure.event.crawler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

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
                new ObjectMapper(),
                new CrawlerJobProperties("crawl.job.create")
        );
    }

    @Test
    @DisplayName("크롤링 작업 생성 메시지를 지정 토픽으로 발행한다")
    void publishSendsMessageToConfiguredTopic() {
        CrawlJobCreateMessage message = new CrawlJobCreateMessage(
                "req-1",
                "clien",
                "키워드",
                3
        );
        when(kafkaTemplate.send(
                eq("crawl.job.create"),
                eq("req-1"),
                eq("{\"client_request_id\":\"req-1\",\"site\":\"clien\",\"keyword\":\"키워드\",\"max_pages\":3}")
        )).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(crawlJobEventPublisher.publish(message))
                .verifyComplete();

        verify(kafkaTemplate).send(
                "crawl.job.create",
                "req-1",
                "{\"client_request_id\":\"req-1\",\"site\":\"clien\",\"keyword\":\"키워드\",\"max_pages\":3}"
        );
    }
}
