package team.hotpotato.domain.audit.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditSuggestionHttpResponse(
        @JsonProperty("start_index") int startIndex,
        @JsonProperty("end_index") int endIndex,
        String before,
        String after,
        String reason
) {
}
