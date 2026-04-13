package team.hotpotato.domain.reaction.application.community;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CrawlJobCreateMessage(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("keyword") String keyword,
        @JsonProperty("max_pages") Integer maxPages
) {
}
