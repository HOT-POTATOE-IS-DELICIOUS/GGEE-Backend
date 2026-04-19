package team.hotpotato.domain.issue.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueConnectionHttpResponse(
        @JsonProperty("source_id") String sourceId,
        @JsonProperty("target_id") String targetId,
        Double similarity
) {
}
