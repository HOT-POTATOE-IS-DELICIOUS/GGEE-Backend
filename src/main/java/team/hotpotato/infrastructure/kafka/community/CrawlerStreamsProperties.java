package team.hotpotato.infrastructure.kafka.community;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ggee.crawler")
public record CrawlerStreamsProperties(
        String dedupStoreName,
        Duration dedupTtl,
        Duration cleanupInterval
) {
}
