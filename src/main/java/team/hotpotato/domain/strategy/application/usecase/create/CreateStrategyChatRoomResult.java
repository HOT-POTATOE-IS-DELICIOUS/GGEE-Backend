package team.hotpotato.domain.strategy.application.usecase.create;

import java.time.LocalDateTime;

public record CreateStrategyChatRoomResult(
        Long roomId,
        LocalDateTime lastChattedAt,
        LocalDateTime createdAt
) {
}
