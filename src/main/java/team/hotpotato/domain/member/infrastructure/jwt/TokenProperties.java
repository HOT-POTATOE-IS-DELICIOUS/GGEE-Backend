package team.hotpotato.domain.member.infrastructure.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("jwt")
public record TokenProperties(
        long accessTokenActiveTime,
        long refreshTokenActiveTime,
        String prefix,
        String header,
        String secretKey
) {
    public Duration accessTokenActiveDuration() {
        return Duration.ofMillis(accessTokenActiveTime);
    }

    public Duration refreshTokenActiveDuration() {
        return Duration.ofMillis(refreshTokenActiveTime);
    }
}
