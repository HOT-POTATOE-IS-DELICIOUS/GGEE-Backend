package team.hotpotato.domain.audit.application.dto;

import team.hotpotato.domain.audit.domain.AuditReview;

import java.util.List;

public record AuditResult(
        Long auditId,
        String messageId,
        List<AuditReview> reviews
) {
}
