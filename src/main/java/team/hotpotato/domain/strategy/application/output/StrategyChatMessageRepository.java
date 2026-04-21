package team.hotpotato.domain.strategy.application.output;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.strategy.domain.StrategyChatMessage;

public interface StrategyChatMessageRepository {
    Mono<StrategyChatMessage> save(StrategyChatMessage message);
    Flux<StrategyChatMessage> findAllByRoomId(Long roomId);
}
