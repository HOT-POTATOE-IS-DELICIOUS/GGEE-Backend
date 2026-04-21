package team.hotpotato.domain.strategy.application.input;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface CreateAndStreamStrategyChat {
    Flux<ServerSentEvent<String>> createAndStream(Long userId, String message);
}
