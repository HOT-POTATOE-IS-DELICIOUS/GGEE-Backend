package team.hotpotato.domain.strategy.application.input;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import team.hotpotato.domain.strategy.application.usecase.stream.StreamStrategyChatCommand;

public interface StreamStrategyChat {
    Flux<ServerSentEvent<String>> stream(StreamStrategyChatCommand command);
}
