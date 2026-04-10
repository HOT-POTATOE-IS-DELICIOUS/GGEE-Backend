package team.hotpotato.domain.member.domain;

import java.time.LocalDateTime;

public record ProtectTargetIndexingOutbox(
        Long id,
        String protectTarget,
        ProtectTargetIndexingOutboxStatus status,
        LocalDateTime publishedAt
) {
}
