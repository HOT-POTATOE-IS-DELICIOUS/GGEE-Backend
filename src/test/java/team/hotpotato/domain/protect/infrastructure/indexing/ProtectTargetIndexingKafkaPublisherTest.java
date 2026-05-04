package team.hotpotato.domain.protect.infrastructure.indexing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.test.StepVerifier;
import team.hotpotato.domain.protect.application.dto.ProtectTargetIndexingPublishCommand;
import team.hotpotato.infrastructure.crawler.CrawlerTopics;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호 대상 인덱싱 이벤트 퍼블리셔 단위 테스트")
class ProtectTargetIndexingKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("보호 대상 인덱싱 이벤트를 지정한 토픽으로 발행한다")
    void publishSendsMessageToKafka() throws Exception {
        ProtectTargetIndexingKafkaPublisher publisher =
                new ProtectTargetIndexingKafkaPublisher(kafkaTemplate, new ObjectMapper());
        ProtectTargetIndexingPublishCommand command =
                new ProtectTargetIndexingPublishCommand(1L, "brand", "브랜드 공식몰");

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(completedFuture());

        StepVerifier.create(publisher.publish(command))
                .verifyComplete();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(CrawlerTopics.CRAWL_REQUEST);
        assertThat(keyCaptor.getValue()).isEqualTo("1");
        assertThat(payloadCaptor.getValue())
                .isEqualTo("{\"job_id\":1,\"keyword\":\"brand\",\"protect_target_info\":\"브랜드 공식몰\"}");
    }

    @Test
    @DisplayName("직렬화에 실패하면 예외를 반환한다")
    void publishFailsWhenSerializationFails() throws Exception {
        ProtectTargetIndexingKafkaPublisher publisher =
                new ProtectTargetIndexingKafkaPublisher(kafkaTemplate, objectMapper);
        ProtectTargetIndexingPublishCommand command =
                new ProtectTargetIndexingPublishCommand(1L, "brand", "브랜드 공식몰");

        when(objectMapper.writeValueAsString(any(ProtectTargetIndexingKafkaMessage.class)))
                .thenThrow(new JsonProcessingException("serialization failed") {
                });

        StepVerifier.create(publisher.publish(command))
                .expectErrorMatches(error ->
                        error instanceof IllegalStateException
                                && error.getMessage().contains("직렬화")
                )
                .verify();

        verifyNoInteractions(kafkaTemplate);
    }

    private CompletableFuture<SendResult<String, String>> completedFuture() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }
}
