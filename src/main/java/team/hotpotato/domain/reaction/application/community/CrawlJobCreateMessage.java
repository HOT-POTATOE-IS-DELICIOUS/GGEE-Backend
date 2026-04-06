package team.hotpotato.domain.reaction.application.community;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CrawlJobCreateMessage(
        @JsonProperty("client_request_id") String clientRequestId,
        @JsonProperty("site") String site,
        @JsonProperty("keyword") String keyword,
        @JsonProperty("max_pages") Integer maxPages
) {
}
