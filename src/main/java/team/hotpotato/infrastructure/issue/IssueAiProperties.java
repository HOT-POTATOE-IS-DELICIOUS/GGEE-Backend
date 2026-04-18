package team.hotpotato.infrastructure.issue;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ggee.ai.issue")
public record IssueAiProperties(
        String baseUrl,
        Duration timeout
) {
}
