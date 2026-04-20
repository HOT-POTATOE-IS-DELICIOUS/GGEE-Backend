package team.hotpotato.domain.strategy.api.dto;

import java.time.LocalDateTime;

public record CreateRoomResponse(
        Long roomId,
        LocalDateTime lastChattedAt,
        LocalDateTime createdAt
) {
}
