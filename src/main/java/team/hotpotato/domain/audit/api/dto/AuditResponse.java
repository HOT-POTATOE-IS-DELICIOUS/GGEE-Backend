package team.hotpotato.domain.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AuditResponse(
        @JsonProperty("audit_id") Long auditId,
        @JsonProperty("message_id") String messageId,
        List<AuditReviewResponse> reviews
) {
}
