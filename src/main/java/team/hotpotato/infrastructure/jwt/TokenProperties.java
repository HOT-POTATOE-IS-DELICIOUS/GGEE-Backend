package team.hotpotato.infrastructure.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt")
public record TokenProperties(
        long accessTokenActiveTime,
        long refreshTokenActiveTime,
        String prefix,
        String header,
        String secretKey
) {
}
