package team.hotpotato.domain.audit.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AuditReviewHttpResponse(
        AuditSentenceHttpResponse sentence,
        @JsonProperty("perspective_ids") List<String> perspectiveIds,
        @JsonProperty("perspective_labels") List<String> perspectiveLabels,
        List<AuditSuggestionHttpResponse> suggestions
) {
}
