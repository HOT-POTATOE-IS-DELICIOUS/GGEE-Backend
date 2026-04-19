package team.hotpotato.domain.issue.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IssuesResponse(
        @JsonProperty("protect_target") String protectTarget,
        List<IssueNodeResponse> issues,
        List<IssueConnectionResponse> connections
) {
}
