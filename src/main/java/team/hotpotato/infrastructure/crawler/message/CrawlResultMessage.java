package team.hotpotato.infrastructure.crawler.message;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlResultMessage(
        String jobId,
        String timestamp,
        String status,
        String site,
        String keyword,
        Integer totalUrls,
        Integer processedCount,
        Integer successCount,
        Integer failedCount,
        List<CrawlPostMessage> results
) {
}
