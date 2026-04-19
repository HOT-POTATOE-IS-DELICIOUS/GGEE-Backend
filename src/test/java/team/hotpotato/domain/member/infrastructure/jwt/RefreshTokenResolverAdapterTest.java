package team.hotpotato.domain.member.infrastructure.jwt;

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

@DisplayName("리프레시 JWT 토큰 파싱 단위 테스트")
class RefreshTokenResolverAdapterTest {

    private static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctMTIz";
    private static final String TEST_SESSION_ID = "test-refresh-session-123";

    private TokenGeneratorAdapter tokenGeneratorAdapter;
    private RefreshTokenResolverAdapter refreshTokenResolverAdapter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET_KEY));
        TokenProperties tokenProperties = new TokenProperties(
                3_600_000L, 1_209_600_000L, "Bearer ", "Authorization", TEST_SECRET_KEY
        );
        tokenGeneratorAdapter = new TokenGeneratorAdapter(secretKey, tokenProperties);
        TokenValidatorAdapter tokenValidatorAdapter = new TokenValidatorAdapter(tokenProperties);
        refreshTokenResolverAdapter = new RefreshTokenResolverAdapter(tokenValidatorAdapter, secretKey);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰은 올바른 AuthPrincipal을 반환한다")
    void resolveReturnsCorrectPrincipalForValidRefreshToken() {
        String token = tokenGeneratorAdapter.generateRefreshToken(new AuthPrincipal(42L, Role.USER, TEST_SESSION_ID));

        StepVerifier.create(refreshTokenResolverAdapter.resolve(token))
                .assertNext(principal -> {
                    assertThat(principal.userId()).isEqualTo(42L);
                    assertThat(principal.role()).isEqualTo(Role.USER);
                    assertThat(principal.sessionId()).isEqualTo(TEST_SESSION_ID);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("만료된 리프레시 토큰은 ExpiredRefreshTokenException을 반환한다")
    void resolveThrowsExpiredRefreshTokenForExpiredToken() {
        String expiredRefreshToken = Jwts.builder()
                .signWith(secretKey)
                .subject("99")
                .claim("role", "ROLE_USER")
                .claim("tokenType", TokenType.REFRESH_TOKEN.name())
                .claim("sessionId", "expired-refresh-session")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .compact();

        StepVerifier.create(refreshTokenResolverAdapter.resolve(expiredRefreshToken))
                .expectError(ExpiredRefreshTokenException.class)
                .verify();
    }

    @Test
    @DisplayName("액세스 토큰은 InvalidTokenTypeException을 반환한다")
    void resolveThrowsInvalidTokenTypeForAccessToken() {
        String accessToken = tokenGeneratorAdapter.generateAccessToken(new AuthPrincipal(7L, Role.USER, TEST_SESSION_ID));

        StepVerifier.create(refreshTokenResolverAdapter.resolve(accessToken))
                .expectError(InvalidTokenTypeException.class)
                .verify();
    }

    @Test
    @DisplayName("변조된 JWT는 InvalidTokenException을 반환한다")
    void resolveThrowsInvalidTokenForTamperedToken() {
        String validRefreshToken = tokenGeneratorAdapter.generateRefreshToken(
                new AuthPrincipal(7L, Role.USER, TEST_SESSION_ID)
        );
        String tamperedRefreshToken = validRefreshToken.substring(0, validRefreshToken.length() - 2) + "ab";

        StepVerifier.create(refreshTokenResolverAdapter.resolve(tamperedRefreshToken))
                .expectError(InvalidTokenException.class)
                .verify();
    }
}
