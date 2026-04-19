package team.hotpotato.domain.audit.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ggee.ai.audit")
public record AuditAiProperties(
        String baseUrl,
        Duration timeout
) {
}
