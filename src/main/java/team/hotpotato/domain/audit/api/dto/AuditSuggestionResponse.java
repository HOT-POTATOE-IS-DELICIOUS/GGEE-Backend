package team.hotpotato.domain.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditSuggestionResponse(
        @JsonProperty("start_index") int startIndex,
        @JsonProperty("end_index") int endIndex,
        String before,
        String after,
        String reason
) {
}
