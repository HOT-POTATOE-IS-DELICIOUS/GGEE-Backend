package team.hotpotato.infrastructure.jwt;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.TokenGenerator;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenGeneratorAdapter implements TokenGenerator {
    private final SecretKey secretKey;
    private final TokenProperties tokenProperties;

    @Override
    public String generateAccessToken(AuthPrincipal authPrincipal) {
        return buildToken(
                TokenType.ACCESS_TOKEN,
                authPrincipal.userId(),
                authPrincipal.role().name(),
                tokenProperties.accessTokenActiveTime()
        );
    }

    @Override
    public String generateRefreshToken(AuthPrincipal authPrincipal) {
        return buildToken(
                TokenType.REFRESH_TOKEN,
                authPrincipal.userId(),
                authPrincipal.role().name(),
                tokenProperties.refreshTokenActiveTime()
        );
    }

    private String buildToken(TokenType tokenType, Long userId, String role, long tokenActiveTime) {
        Date now = new Date();
        return Jwts.builder()
                .signWith(secretKey)
                .header()
                .type("jwt")
                .and()
                .subject(userId.toString())
                .claim("role", role)
                .claim("tokenType", tokenType.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + tokenActiveTime))
                .compact();
    }
}
