package team.hotpotato.domain.audit.domain;

import java.util.List;

public record AuditAnalysis(
        List<AuditReview> reviews
) {
}
