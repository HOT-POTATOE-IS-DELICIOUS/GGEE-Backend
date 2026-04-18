package team.hotpotato.infrastructure.event.crawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.community.CrawlJobCreateMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlJobEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String CRAWL_JOB_TOPIC = "crawl.request";

    public Mono<Void> publish(CrawlJobCreateMessage message) {
        return serialize(message)
                .flatMap(payload -> Mono.fromFuture(
                        kafkaTemplate.send(
								CRAWL_JOB_TOPIC,
                                message.jobId(),
                                payload
                        )
                ))
                .doOnSuccess(result -> log.info(
                        "크롤링 작업 생성 요청을 발행했습니다. topic={}, jobId={}",
						CRAWL_JOB_TOPIC,
                        message.jobId()
                ))
                .then();
    }

    private Mono<String> serialize(CrawlJobCreateMessage message) {
        try {
            return Mono.just(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            return Mono.error(new IllegalStateException("크롤링 작업 생성 요청 직렬화에 실패했습니다.", exception));
        }
    }
}
