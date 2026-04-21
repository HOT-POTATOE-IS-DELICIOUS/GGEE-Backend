package team.hotpotato.domain.reaction.infrastructure.comment;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeduplicatedPostMessage(
        @JsonProperty("post_id") Long postId,
        String site,
        String keyword,
        @JsonProperty("crawled_at") OffsetDateTime crawledAt,
        @JsonProperty("event_timestamp_ms") Long eventTimestampMs,
        @JsonProperty("post_url") String postUrl,
        @JsonProperty("post_title") String postTitle
) {
}
