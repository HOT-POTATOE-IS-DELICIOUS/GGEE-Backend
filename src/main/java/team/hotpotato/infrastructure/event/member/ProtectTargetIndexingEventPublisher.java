package team.hotpotato.infrastructure.event.member;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.event.ProtectTargetIndexingMessage;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.infrastructure.kafka.EventTopics;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingEventPublisher implements ProtectTargetIndexingPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> publish(ProtectTargetIndexingMessage message) {
        return serialize(message)
                .flatMap(payload -> Mono.fromFuture(
                        kafkaTemplate.send(
                                EventTopics.CRAWL_REQUEST,
                                message.jobId(),
                                payload
                        )
                ))
                .doOnSuccess(result -> log.info(
                        "보호 대상 인덱싱 요청을 발행했습니다. topic={}, jobId={}, keyword={}",
                        EventTopics.CRAWL_REQUEST,
                        message.jobId(),
                        message.keyword()
                ))
                .then();
    }

    private Mono<String> serialize(ProtectTargetIndexingMessage message) {
        try {
            return Mono.just(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            return Mono.error(new IllegalStateException("보호 대상 인덱싱 요청 직렬화에 실패했습니다.", exception));
        }
    }
}
