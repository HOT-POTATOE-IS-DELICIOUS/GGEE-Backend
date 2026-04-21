package team.hotpotato.domain.reaction.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ggee.ai.reaction")
public record ReactionAiProperties(
        String baseUrl,
        Duration timeout
) {}
