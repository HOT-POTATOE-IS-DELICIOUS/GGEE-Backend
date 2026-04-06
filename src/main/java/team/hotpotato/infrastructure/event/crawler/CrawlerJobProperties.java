package team.hotpotato.infrastructure.event.crawler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ggee.crawler.job")
public record CrawlerJobProperties(
        String createTopic
) {
}
