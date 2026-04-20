package team.hotpotato.domain.strategy.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ggee.ai.strategy")
public record StrategyAiProperties(
        String baseUrl
) {
}
