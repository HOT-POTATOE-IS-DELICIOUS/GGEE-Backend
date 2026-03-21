package team.hotpotato.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.hotpotato.domain.member.application.auth.AuthPrincipal;
import team.hotpotato.domain.member.domain.Role;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT 토큰 생성 단위 테스트")
class TokenGeneratorAdapterTest {

    private static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctMTIz";
    private static final long ACCESS_TOKEN_ACTIVE_TIME = 3_600_000L;
    private static final long REFRESH_TOKEN_ACTIVE_TIME = 1_209_600_000L;

    private TokenGeneratorAdapter tokenGeneratorAdapter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET_KEY));
        TokenProperties tokenProperties = new TokenProperties(
                ACCESS_TOKEN_ACTIVE_TIME, REFRESH_TOKEN_ACTIVE_TIME, "Bearer ", "Authorization", TEST_SECRET_KEY
        );
        tokenGeneratorAdapter = new TokenGeneratorAdapter(secretKey, tokenProperties);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    @DisplayName("액세스 토큰은 id, ROLE_ 접두사 role, ACCESS_TOKEN 타입 클레임을 포함한다")
    void accessTokenContainsCorrectClaims() {
        AuthPrincipal principal = new AuthPrincipal(42L, Role.USER);

        String token = tokenGeneratorAdapter.generateAccessToken(principal);

        Claims claims = parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("tokenType", String.class)).isEqualTo(TokenType.ACCESS_TOKEN.name());
    }

    @Test
    @DisplayName("리프레시 토큰은 REFRESH_TOKEN 타입 클레임을 포함한다")
    void refreshTokenContainsCorrectClaims() {
        AuthPrincipal principal = new AuthPrincipal(42L, Role.USER);

        String token = tokenGeneratorAdapter.generateRefreshToken(principal);

        Claims claims = parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("tokenType", String.class)).isEqualTo(TokenType.REFRESH_TOKEN.name());
    }

    @Test
    @DisplayName("액세스 토큰 만료 시각은 발급 시각으로부터 accessTokenActiveTime 이내다")
    void accessTokenExpirationIsWithinActiveTime() {
        Date before = new Date();
        String token = tokenGeneratorAdapter.generateAccessToken(new AuthPrincipal(1L, Role.USER));
        Date after = new Date();

        Claims claims = parseClaims(token);
        assertThat(claims.getExpiration()).isAfter(before);
        assertThat(claims.getExpiration().getTime())
                .isLessThanOrEqualTo(after.getTime() + ACCESS_TOKEN_ACTIVE_TIME);
    }

    @Test
    @DisplayName("리프레시 토큰 만료 시각은 액세스 토큰보다 길다")
    void refreshTokenExpiresLaterThanAccessToken() {
        AuthPrincipal principal = new AuthPrincipal(1L, Role.USER);

        Date accessExpiry = parseClaims(tokenGeneratorAdapter.generateAccessToken(principal)).getExpiration();
        Date refreshExpiry = parseClaims(tokenGeneratorAdapter.generateRefreshToken(principal)).getExpiration();

        assertThat(refreshExpiry).isAfter(accessExpiry);
    }
}
