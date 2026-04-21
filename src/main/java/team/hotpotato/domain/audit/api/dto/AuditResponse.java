package team.hotpotato.domain.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AuditResponse(
        @JsonProperty("audit_id") Long auditId,
        List<AuditReviewResponse> reviews
) {
}
