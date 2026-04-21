package team.hotpotato.domain.strategy.application.usecase.create;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.strategy.application.input.CreateStrategyChatRoom;
import team.hotpotato.domain.strategy.application.output.StrategyChatRoomRepository;
import team.hotpotato.domain.strategy.domain.StrategyChatRoom;

@Service
@RequiredArgsConstructor
public class CreateStrategyChatRoomUseCase implements CreateStrategyChatRoom {

    private final StrategyChatRoomRepository roomRepository;
    private final IdGenerator idGenerator;

    @Override
    public Mono<CreateStrategyChatRoomResult> create(CreateStrategyChatRoomCommand command) {
        return roomRepository.save(new StrategyChatRoom(
                        idGenerator.generateId(),
                        command.userId(),
                        "",
                        null,
                        LocalDateTime.now()
                ))
                .map(saved -> new CreateStrategyChatRoomResult(saved.id()));
    }
}
