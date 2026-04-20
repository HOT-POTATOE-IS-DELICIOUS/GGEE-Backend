package team.hotpotato.domain.strategy.application.query.rooms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import team.hotpotato.domain.strategy.application.input.GetStrategyChatRooms;
import team.hotpotato.domain.strategy.application.output.StrategyChatRoomRepository;

@Service
@RequiredArgsConstructor
public class GetStrategyChatRoomsQuery implements GetStrategyChatRooms {

    private final StrategyChatRoomRepository roomRepository;

    @Override
    public Flux<StrategyChatRoomsResult> getRooms(Long userId) {
        return roomRepository.findAllByUserId(userId)
                .map(room -> new StrategyChatRoomsResult(
                        room.id(),
                        room.lastChattedAt(),
                        room.createdAt()
                ));
    }
}
