package team.hotpotato.domain.audit.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AuditHttpResponse(
        @JsonProperty("message_id") String messageId,
        List<AuditReviewHttpResponse> reviews
) {
}
