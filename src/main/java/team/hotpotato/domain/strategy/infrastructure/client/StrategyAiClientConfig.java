package team.hotpotato.domain.strategy.infrastructure.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StrategyAiProperties.class)
public class StrategyAiClientConfig {
}
