package team.hotpotato.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.auth.AuthPrincipal;
import team.hotpotato.domain.member.application.auth.TokenResolver;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class TokenResolverAdapter implements TokenResolver {
    private final TokenValidatorAdapter tokenValidatorAdapter;
    private final TokenProperties tokenProperties;
    private final SecretKey secretKey;

    @Override
    public Mono<AuthPrincipal> resolve(String authorizationHeader) {
        return stripBearerScheme(authorizationHeader).flatMap(this::getPrincipal);
    }

    private Mono<String> stripBearerScheme(String headerValue) {
        return Mono.fromCallable(() -> {
            tokenValidatorAdapter.validateAuthorizationHeader(headerValue);
            return headerValue.substring(tokenProperties.prefix().length()).trim();
        });
    }

    private Mono<AuthPrincipal> getPrincipal(String token) {
        return getTokenBody(token)
                .map(claims -> {
                    Long userId = Long.parseLong(claims.getSubject());
                    String role = claims.get("role", String.class);
                    return new AuthPrincipal(userId, role);
                });
    }

    private Mono<Claims> getTokenBody(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String tokenTypeValue = claims.get("tokenType", String.class);
                TokenType tokenType = TokenType.valueOf(tokenTypeValue);
                tokenValidatorAdapter.validateTokenType(tokenType, TokenType.ACCESS_TOKEN);

                return claims;
            } catch (InvalidTokenTypeException e) {
                throw InvalidTokenTypeException.EXCEPTION;
            } catch (ExpiredJwtException e) {
                throw ExpiredTokenException.EXCEPTION;
            } catch (JwtException e) {
                throw InvalidTokenException.EXCEPTION;
            }
        });
    }
}
