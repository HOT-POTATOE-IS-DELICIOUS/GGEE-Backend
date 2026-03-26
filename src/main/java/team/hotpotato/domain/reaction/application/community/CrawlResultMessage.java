package team.hotpotato.domain.reaction.application.community;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlResultMessage(
        @JsonProperty("job_id") String jobId,
        String timestamp,
        String status,
        String site,
        String keyword,
        @JsonProperty("total_urls") Integer totalUrls,
        @JsonProperty("processed_count") Integer processedCount,
        @JsonProperty("success_count") Integer successCount,
        @JsonProperty("failed_count") Integer failedCount,
        List<CrawlPostMessage> results
) {
}
