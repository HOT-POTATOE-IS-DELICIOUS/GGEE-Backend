package team.hotpotato.domain.strategy.application.input;

import reactor.core.publisher.Flux;
import team.hotpotato.domain.strategy.application.query.messages.StrategyChatMessagesResult;

public interface GetStrategyChatMessages {
    Flux<StrategyChatMessagesResult> getMessages(Long roomId, Long userId);
}
