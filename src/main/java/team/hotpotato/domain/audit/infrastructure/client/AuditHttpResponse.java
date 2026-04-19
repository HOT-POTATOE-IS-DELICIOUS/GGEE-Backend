package team.hotpotato.domain.audit.infrastructure.client;

import java.util.List;

public record AuditHttpResponse(
        List<AuditReviewHttpResponse> reviews
) {
}
