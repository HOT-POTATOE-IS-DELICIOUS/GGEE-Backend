package team.hotpotato.domain.reaction.application.community;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlCommentMessage(
        Integer id,
        @JsonProperty("parent_id") Integer parentId,
        String author,
        String date,
        String content,
        String likes,
        String dislikes
) {
}
