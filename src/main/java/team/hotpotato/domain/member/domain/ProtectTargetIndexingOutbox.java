package team.hotpotato.domain.member.domain;

import java.time.LocalDateTime;

public record ProtectTargetIndexingOutbox(
        Long id,
        String protectTarget,
        String protectTargetInfo,
        ProtectTargetIndexingOutboxStatus status,
        LocalDateTime publishedAt
) {
}
