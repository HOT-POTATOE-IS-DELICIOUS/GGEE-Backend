package team.hotpotato.domain.strategy.infrastructure.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Flux;
import team.hotpotato.domain.strategy.application.output.StrategyAiClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyAiClientAdapter implements StrategyAiClient {

    private final WebClient.Builder webClientBuilder;
    private final StrategyAiProperties properties;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Flux<ServerSentEvent<String>> stream(String message, String entityName, String entityInfo) {
        return webClient
                .post()
                .uri("/strategy/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new StrategyAiRequest(message, entityName, entityInfo))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnError(WebClientException.class, e -> log.warn("전략 AI 스트림 연결 오류", e));
    }
}
