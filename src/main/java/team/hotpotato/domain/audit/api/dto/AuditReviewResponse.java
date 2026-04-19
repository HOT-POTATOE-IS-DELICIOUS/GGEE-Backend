package team.hotpotato.domain.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AuditReviewResponse(
        AuditSentenceResponse sentence,
        @JsonProperty("perspective_ids") List<String> perspectiveIds,
        @JsonProperty("perspective_labels") List<String> perspectiveLabels,
        List<AuditSuggestionResponse> suggestions
) {
}
