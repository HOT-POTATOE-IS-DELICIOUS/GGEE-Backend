package team.hotpotato.domain.strategy.application.query.rooms;

import java.time.LocalDateTime;

public record StrategyChatRoomsResult(
        Long roomId,
        String title,
        LocalDateTime lastChattedAt,
        LocalDateTime createdAt
) {
}
