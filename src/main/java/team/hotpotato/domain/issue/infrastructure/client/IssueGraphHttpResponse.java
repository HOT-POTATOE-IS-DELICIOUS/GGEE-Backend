package team.hotpotato.domain.issue.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IssueGraphHttpResponse(
        @JsonProperty("entity_name") String entityName,
        List<IssueNodeHttpResponse> issues,
        List<IssueConnectionHttpResponse> connections
) {
}
