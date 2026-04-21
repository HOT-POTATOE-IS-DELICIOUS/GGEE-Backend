package team.hotpotato.domain.strategy.api.dto;

import java.time.LocalDateTime;

public record RoomResponse(
        Long roomId,
        String title,
        LocalDateTime lastChattedAt,
        LocalDateTime createdAt
) {
}
