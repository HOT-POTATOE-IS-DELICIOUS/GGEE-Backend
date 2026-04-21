package team.hotpotato.domain.reaction.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NodeNewsHttpResponse(
        @JsonProperty("node_id") String nodeId,
        Integer count,
        List<NewsItemHttpResponse> news
) {}
