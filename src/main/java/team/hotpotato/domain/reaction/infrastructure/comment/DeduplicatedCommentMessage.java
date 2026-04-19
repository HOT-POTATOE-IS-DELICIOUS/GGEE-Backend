package team.hotpotato.domain.reaction.infrastructure.comment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeduplicatedCommentMessage(
        @JsonProperty("post_id") Long postId,
        Integer id,
        @JsonProperty("parent_id") Integer parentId,
        String author,
        String date,
        String content,
        String likes,
        String dislikes
) {
}
