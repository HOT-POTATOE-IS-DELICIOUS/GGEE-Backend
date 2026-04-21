package team.hotpotato.domain.reaction.infrastructure.comment;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.hotpotato.infrastructure.crawler.message.CrawlCommentMessage;

public record CommentDedupResult(
        @JsonProperty("post_id") Long postId,
        String site,
        String keyword,
        @JsonProperty("crawled_at") OffsetDateTime crawledAt,
        @JsonProperty("event_timestamp_ms") Long eventTimestampMs,
        @JsonProperty("post_url") String postUrl,
        @JsonProperty("post_title") String postTitle,
        @JsonProperty("new_comments") List<CrawlCommentMessage> newComments
) {
}
