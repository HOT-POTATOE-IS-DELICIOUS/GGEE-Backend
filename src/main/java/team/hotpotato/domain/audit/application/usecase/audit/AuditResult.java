package team.hotpotato.domain.audit.application.usecase.audit;

import team.hotpotato.domain.audit.domain.AuditReview;

import java.util.List;

public record AuditResult(
        Long auditId,
        String messageId,
        List<AuditReview> reviews
) {
}
