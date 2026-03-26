package team.hotpotato.domain.reaction.application.community;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeduplicatedCommentMessage(
        String id,
        String site,
        String keyword,
        @JsonProperty("crawled_at") String crawledAt,
        @JsonProperty("event_timestamp_ms") Long eventTimestampMs,
        @JsonProperty("post_url") String postUrl,
        @JsonProperty("post_title") String postTitle,
        @JsonProperty("comment_id") Integer commentId,
        @JsonProperty("parent_id") Integer parentId,
        String author,
        @JsonProperty("comment_date") String commentDate,
        String content,
        String likes,
        String dislikes
) {
}
