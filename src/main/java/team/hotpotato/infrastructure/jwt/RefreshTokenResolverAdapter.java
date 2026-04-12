package team.hotpotato.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.input.RefreshTokenResolver;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.domain.Role;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class RefreshTokenResolverAdapter implements RefreshTokenResolver {
    private static final String ROLE_PREFIX = "ROLE_";
    private final TokenValidatorAdapter tokenValidatorAdapter;
    private final SecretKey secretKey;

    @Override
    public Mono<AuthPrincipal> resolve(String refreshToken) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(refreshToken)
                        .getPayload();

                String tokenTypeValue = claims.get("tokenType", String.class);
                TokenType tokenType = TokenType.valueOf(tokenTypeValue);
                tokenValidatorAdapter.validateTokenType(tokenType, TokenType.REFRESH_TOKEN);

                Long userId = Long.parseLong(claims.getSubject());
                Role role = Role.valueOf(normalizeRole(claims.get("role", String.class)));
                String sessionId = claims.get("sessionId", String.class);

                return new AuthPrincipal(userId, role, sessionId);
            } catch (InvalidTokenTypeException e) {
                throw InvalidTokenTypeException.EXCEPTION;
            } catch (ExpiredJwtException e) {
                throw ExpiredRefreshTokenException.EXCEPTION;
            } catch (JwtException | IllegalArgumentException | NullPointerException e) {
                throw InvalidTokenException.EXCEPTION;
            }
        });
    }

    private String normalizeRole(String roleClaim) {
        if (roleClaim.startsWith(ROLE_PREFIX)) {
            return roleClaim.substring(ROLE_PREFIX.length());
        }
        return roleClaim;
    }
}
