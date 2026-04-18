package team.hotpotato.infrastructure.member.indexing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.dto.ProtectTargetIndexingPublishCommand;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.infrastructure.crawler.CrawlerTopics;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingKafkaPublisher implements ProtectTargetIndexingPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> publish(ProtectTargetIndexingPublishCommand command) {
        return serialize(new ProtectTargetIndexingKafkaMessage(
                        command.jobId(),
                        command.keyword(),
                        command.protectTargetInfo()
                ))
                .flatMap(payload -> Mono.fromFuture(
                        kafkaTemplate.send(
                                CrawlerTopics.CRAWL_REQUEST,
                                String.valueOf(command.jobId()),
                                payload
                        )
                ))
                .doOnSuccess(result -> log.info(
                        "보호 대상 인덱싱 요청을 발행했습니다. topic={}, jobId={}, keyword={}, protectTargetInfo={}",
                        CrawlerTopics.CRAWL_REQUEST,
                        command.jobId(),
                        command.keyword(),
                        command.protectTargetInfo()
                ))
                .then();
    }

    private Mono<String> serialize(ProtectTargetIndexingKafkaMessage message) {
        try {
            return Mono.just(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            return Mono.error(new IllegalStateException("보호 대상 인덱싱 요청 직렬화에 실패했습니다.", exception));
        }
    }
}
