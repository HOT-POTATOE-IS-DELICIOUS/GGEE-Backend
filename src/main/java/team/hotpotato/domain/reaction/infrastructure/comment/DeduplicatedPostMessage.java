package team.hotpotato.domain.reaction.infrastructure.comment;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.hotpotato.infrastructure.crawler.message.CrawlCommentMessage;

public record DeduplicatedPostMessage(
        String site,
        String keyword,
        @JsonProperty("crawled_at") String crawledAt,
        @JsonProperty("event_timestamp_ms") Long eventTimestampMs,
        @JsonProperty("post_url") String postUrl,
        @JsonProperty("post_title") String postTitle,
        @JsonProperty("new_comments") List<CrawlCommentMessage> newComments
) {
}
