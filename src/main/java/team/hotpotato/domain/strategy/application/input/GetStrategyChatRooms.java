package team.hotpotato.domain.strategy.application.input;

import reactor.core.publisher.Flux;
import team.hotpotato.domain.strategy.application.query.rooms.StrategyChatRoomsResult;

public interface GetStrategyChatRooms {
    Flux<StrategyChatRoomsResult> getRooms(Long userId);
}
