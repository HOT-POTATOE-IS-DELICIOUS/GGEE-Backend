package team.hotpotato.domain.strategy.domain;

import java.time.LocalDateTime;

public record StrategyChatRoom(
        Long id,
        Long userId,
        LocalDateTime lastChattedAt,
        LocalDateTime createdAt
) {
}
