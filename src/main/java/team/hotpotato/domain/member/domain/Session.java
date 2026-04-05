package team.hotpotato.domain.member.domain;

import java.time.LocalDateTime;

public record Session(
        Long id,
        Long userId,
        String sessionId,
        String refreshToken,
        LocalDateTime expiresAt
) {
}
