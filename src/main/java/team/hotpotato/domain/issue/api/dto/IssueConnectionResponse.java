package team.hotpotato.domain.issue.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueConnectionResponse(
        @JsonProperty("source_id") String sourceId,
        @JsonProperty("target_id") String targetId,
        double similarity
) {
}
