package team.hotpotato.infrastructure.event.member;

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
import team.hotpotato.domain.member.application.event.ProtectTargetIndexingMessage;
import team.hotpotato.infrastructure.kafka.EventTopics;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호 대상 인덱싱 이벤트 퍼블리셔 단위 테스트")
class ProtectTargetIndexingEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("보호 대상 인덱싱 이벤트를 지정한 토픽으로 발행한다")
    void publishSendsMessageToKafka() throws Exception {
        ProtectTargetIndexingEventPublisher publisher =
                new ProtectTargetIndexingEventPublisher(kafkaTemplate, new ObjectMapper());
        ProtectTargetIndexingMessage message = new ProtectTargetIndexingMessage("1", "brand");

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(completedFuture());

        StepVerifier.create(publisher.publish(message))
                .verifyComplete();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(EventTopics.CRAWL_REQUEST);
        assertThat(keyCaptor.getValue()).isEqualTo("1");
        assertThat(payloadCaptor.getValue()).isEqualTo("{\"job_id\":\"1\",\"keyword\":\"brand\"}");
    }

    @Test
    @DisplayName("직렬화에 실패하면 예외를 반환한다")
    void publishFailsWhenSerializationFails() throws Exception {
        ProtectTargetIndexingEventPublisher publisher =
                new ProtectTargetIndexingEventPublisher(kafkaTemplate, objectMapper);
        ProtectTargetIndexingMessage message = new ProtectTargetIndexingMessage("1", "brand");

        when(objectMapper.writeValueAsString(message))
                .thenThrow(new JsonProcessingException("serialization failed") {
                });

        StepVerifier.create(publisher.publish(message))
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
