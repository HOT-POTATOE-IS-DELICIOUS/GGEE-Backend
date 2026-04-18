package team.hotpotato.infrastructure.reaction.comment;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ggee.crawler")
public record CommentDedupStreamProperties(
        String dedupStoreName,
        Duration dedupTtl,
        Duration cleanupInterval
) {
}
