package team.hotpotato.domain.strategy.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.strategy.application.usecase.create.CreateStrategyChatRoomCommand;
import team.hotpotato.domain.strategy.application.usecase.create.CreateStrategyChatRoomResult;

public interface CreateStrategyChatRoom {
    Mono<CreateStrategyChatRoomResult> create(CreateStrategyChatRoomCommand command);
}
