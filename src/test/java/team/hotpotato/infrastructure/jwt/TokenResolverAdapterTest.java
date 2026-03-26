package team.hotpotato.infrastructure.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.domain.Role;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT 토큰 파싱 단위 테스트")
class TokenResolverAdapterTest {

    private static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctMTIz";

    private TokenGeneratorAdapter tokenGeneratorAdapter;
    private TokenResolverAdapter tokenResolverAdapter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET_KEY));
        TokenProperties tokenProperties = new TokenProperties(
                3_600_000L, 1_209_600_000L, "Bearer ", "Authorization", TEST_SECRET_KEY
        );
        tokenGeneratorAdapter = new TokenGeneratorAdapter(secretKey, tokenProperties);
        TokenValidatorAdapter tokenValidatorAdapter = new TokenValidatorAdapter(tokenProperties);
        tokenResolverAdapter = new TokenResolverAdapter(tokenValidatorAdapter, tokenProperties, secretKey);
    }

    @Test
    @DisplayName("유효한 액세스 토큰은 올바른 AuthPrincipal을 반환한다")
    void resolveReturnsCorrectPrincipalForValidAccessToken() {
        String token = tokenGeneratorAdapter.generateAccessToken(new AuthPrincipal(42L, Role.USER));

        StepVerifier.create(tokenResolverAdapter.resolve("Bearer " + token))
                .assertNext(principal -> {
                    assertThat(principal.userId()).isEqualTo(42L);
                    assertThat(principal.role()).isEqualTo(Role.USER);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("리프레시 토큰은 InvalidTokenTypeException을 반환한다")
    void resolveThrowsInvalidTokenTypeForRefreshToken() {
        String token = tokenGeneratorAdapter.generateRefreshToken(new AuthPrincipal(42L, Role.USER));

        StepVerifier.create(tokenResolverAdapter.resolve("Bearer " + token))
                .expectError(InvalidTokenTypeException.class)
                .verify();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 InvalidTokenException을 반환한다")
    void resolveThrowsInvalidTokenForMalformedToken() {
        StepVerifier.create(tokenResolverAdapter.resolve("Bearer invalid.token.value"))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 헤더는 InvalidTokenException을 반환한다")
    void resolveThrowsInvalidTokenWhenPrefixIsMissing() {
        String token = tokenGeneratorAdapter.generateAccessToken(new AuthPrincipal(1L, Role.USER));

        StepVerifier.create(tokenResolverAdapter.resolve(token))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    @DisplayName("이전 ROLE_ 접두사 토큰도 도메인 Role로 정규화한다")
    void resolveNormalizesLegacyRoleClaim() {
        String legacyToken = Jwts.builder()
                .signWith(secretKey)
                .subject("7")
                .claim("role", "ROLE_ADMIN")
                .claim("tokenType", TokenType.ACCESS_TOKEN.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 10_000))
                .compact();

        StepVerifier.create(tokenResolverAdapter.resolve("Bearer " + legacyToken))
                .assertNext(principal -> {
                    assertThat(principal.userId()).isEqualTo(7L);
                    assertThat(principal.role()).isEqualTo(Role.ADMIN);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("만료된 토큰은 ExpiredTokenException을 반환한다")
    void resolveThrowsExpiredTokenForExpiredToken() {
        String expiredToken = Jwts.builder()
                .signWith(secretKey)
                .subject("99")
                .claim("role", "ROLE_USER")
                .claim("tokenType", TokenType.ACCESS_TOKEN.name())
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .compact();

        StepVerifier.create(tokenResolverAdapter.resolve("Bearer " + expiredToken))
                .expectError(ExpiredTokenException.class)
                .verify();
    }
}
