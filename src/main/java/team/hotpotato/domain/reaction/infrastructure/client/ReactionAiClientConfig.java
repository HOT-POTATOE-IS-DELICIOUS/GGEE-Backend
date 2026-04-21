package team.hotpotato.domain.reaction.infrastructure.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReactionAiProperties.class)
public class ReactionAiClientConfig {}
