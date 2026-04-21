package team.hotpotato.domain.strategy.domain;

import java.time.LocalDateTime;

public record StrategyChatRoom(
        Long id,
        Long userId,
        String title,
        LocalDateTime lastChattedAt,
        LocalDateTime createdAt
) {
}
