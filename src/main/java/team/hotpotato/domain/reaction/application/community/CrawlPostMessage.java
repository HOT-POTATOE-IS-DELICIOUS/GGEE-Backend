package team.hotpotato.domain.reaction.application.community;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlPostMessage(
        String title,
        @JsonProperty("comment_count") String commentCount,
        @JsonProperty("view_count") String viewCount,
        @JsonProperty("recommend_count") String recommendCount,
        String date,
        String body,
        List<CrawlCommentMessage> comments,
        String url
) {
}
