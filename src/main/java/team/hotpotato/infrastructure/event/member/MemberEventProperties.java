package team.hotpotato.infrastructure.event.member;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ggee.member")
public record MemberEventProperties(
        String protectTargetIndexingTopic
) {
}
