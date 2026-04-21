package team.hotpotato.domain.strategy.application.output;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface StrategyAiClient {
    Flux<ServerSentEvent<String>> stream(String message, String entityName, String entityInfo);
}
