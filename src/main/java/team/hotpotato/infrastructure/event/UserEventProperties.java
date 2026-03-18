package team.hotpotato.infrastructure.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("event.user")
public record UserEventProperties(
        String registeredTopic
) {
}
