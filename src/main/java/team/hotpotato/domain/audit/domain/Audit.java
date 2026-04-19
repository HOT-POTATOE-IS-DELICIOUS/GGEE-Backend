package team.hotpotato.domain.audit.domain;

import java.util.List;

public record Audit(
        Long auditId,
        Long userId,
        String protectTarget,
        String protectTargetInfo,
        String text,
        String messageId,
        List<AuditReview> reviews
) {
}
