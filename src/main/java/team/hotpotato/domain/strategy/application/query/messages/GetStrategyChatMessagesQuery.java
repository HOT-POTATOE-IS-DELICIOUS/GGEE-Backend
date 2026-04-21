package team.hotpotato.domain.strategy.application.query.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.strategy.application.input.GetStrategyChatMessages;
import team.hotpotato.domain.strategy.application.output.StrategyChatRoomRepository;
import team.hotpotato.domain.strategy.application.output.StrategyChatMessageRepository;
import team.hotpotato.domain.strategy.application.usecase.stream.RoomNotFoundException;

@Service
@RequiredArgsConstructor
public class GetStrategyChatMessagesQuery implements GetStrategyChatMessages {

    private final StrategyChatRoomRepository roomRepository;
    private final StrategyChatMessageRepository messageRepository;

    @Override
    public Flux<StrategyChatMessagesResult> getMessages(Long roomId, Long userId) {
        return roomRepository.findByIdAndUserId(roomId, userId)
                .switchIfEmpty(Mono.error(RoomNotFoundException.EXCEPTION))
                .flatMapMany(room -> messageRepository.findAllByRoomId(roomId))
                .map(msg -> new StrategyChatMessagesResult(
                        msg.id(),
                        msg.role(),
                        msg.content(),
                        msg.intent(),
                        msg.refinedQuery(),
                        msg.metaJson(),
                        msg.createdAt()
                ));
    }
}
