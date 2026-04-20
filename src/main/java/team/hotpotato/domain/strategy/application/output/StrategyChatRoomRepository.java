package team.hotpotato.domain.strategy.application.output;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.strategy.domain.StrategyChatRoom;

public interface StrategyChatRoomRepository {
    Mono<StrategyChatRoom> save(StrategyChatRoom room);
    Mono<StrategyChatRoom> findByIdAndUserId(Long id, Long userId);
    Flux<StrategyChatRoom> findAllByUserId(Long userId);
    Mono<Void> updateLastChattedAt(Long roomId, java.time.LocalDateTime at);
}
