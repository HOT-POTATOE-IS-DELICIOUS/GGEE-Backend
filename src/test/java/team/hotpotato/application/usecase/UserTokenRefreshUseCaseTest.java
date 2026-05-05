package team.hotpotato.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.input.RefreshTokenResolver;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.RefreshTokenHasher;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.usecase.login.InvalidSessionException;
import team.hotpotato.domain.member.application.usecase.login.SessionExpiredException;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshCommand;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshTokenReuseDetectedException;
import team.hotpotato.domain.member.application.usecase.refresh.UserTokenRefreshUseCase;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;
import team.hotpotato.domain.member.infrastructure.jwt.TokenProperties;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("토큰 갱신 유스케이스 단위 테스트")
class UserTokenRefreshUseCaseTest {

    @Mock
    private RefreshTokenResolver refreshTokenResolver;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    private UserTokenRefreshUseCase useCase;

    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String REFRESH_TOKEN_HASH = "hash-of-refresh-token";
    private static final String SESSION_ID = "session-id";
    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(1L, Role.USER, SESSION_ID);

    @BeforeEach
    void setUp() {
        useCase = new UserTokenRefreshUseCase(
                refreshTokenResolver,
                sessionRepository,
                tokenGenerator,
                new TokenProperties(3600L, 1_209_600L, "Bearer", "Authorization", "dummyKey"),
                refreshTokenHasher
        );
    }

    @Test
    @DisplayName("유효한 refresh token과 세션이면 새 토큰 쌍을 반환한다")
    void refreshReturnsNewTokensWhenSessionIsValid() {
        Session validSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN_HASH, LocalDateTime.now().plusDays(14));

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(validSession));
        when(tokenGenerator.generateAccessToken(PRINCIPAL)).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(PRINCIPAL)).thenReturn("new-refresh");
        when(refreshTokenHasher.hash(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("hash-of-new-refresh");
        when(sessionRepository.updateRefreshTokenHash(eq(SESSION_ID), eq(REFRESH_TOKEN_HASH), eq("hash-of-new-refresh"), any(LocalDateTime.class)))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .assertNext(result -> {
                    assertEquals("new-access", result.accessToken());
                    assertEquals("new-refresh", result.refreshToken());
                })
                .verifyComplete();

        verify(sessionRepository).updateRefreshTokenHash(eq(SESSION_ID), eq(REFRESH_TOKEN_HASH), eq("hash-of-new-refresh"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("updateRefreshTokenHash에 갱신된 만료 시간이 전달된다")
    void refreshPassesNewExpiresAtToRepository() {
        Session validSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN_HASH, LocalDateTime.now().plusDays(14));
        ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(validSession));
        when(tokenGenerator.generateAccessToken(any())).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(any())).thenReturn("new-refresh");
        when(refreshTokenHasher.hash(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("hash-of-new-refresh");
        when(sessionRepository.updateRefreshTokenHash(any(), any(), any(), expiresAtCaptor.capture())).thenReturn(Mono.just(1L));

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectNextCount(1)
                .verifyComplete();

        LocalDateTime captured = expiresAtCaptor.getValue();
        LocalDateTime expected = LocalDateTime.now().plusSeconds(1209600L);
        assertEquals(expected.getDayOfYear(), captured.getDayOfYear());
    }

    @Test
    @DisplayName("세션이 존재하지 않으면 InvalidSessionException이 발생한다")
    void refreshFailsWhenSessionNotFound() {
        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(refreshTokenHasher.hash(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("hash-of-new-refresh");
        when(tokenGenerator.generateAccessToken(any())).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(any())).thenReturn("new-refresh");
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectError(InvalidSessionException.class)
                .verify();
    }

    @Test
    @DisplayName("세션이 만료됐으면 SessionExpiredException이 발생한다")
    void refreshFailsWhenSessionExpired() {
        Session expiredSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN_HASH, LocalDateTime.now().minusSeconds(1));

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(refreshTokenHasher.hash(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("hash-of-new-refresh");
        when(tokenGenerator.generateAccessToken(any())).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(any())).thenReturn("new-refresh");
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(expiredSession));

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectError(SessionExpiredException.class)
                .verify();
    }

    @Test
    @DisplayName("refresh token 재사용 탐지 — updateRefreshTokenHash가 0을 반환하면 세션 무효화 후 RefreshTokenReuseDetectedException이 발생한다")
    void refreshDetectsReuseWhenUpdateAffectsZeroRows() {
        Session validSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN_HASH, LocalDateTime.now().plusDays(14));

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(validSession));
        when(tokenGenerator.generateAccessToken(PRINCIPAL)).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(PRINCIPAL)).thenReturn("new-refresh");
        when(refreshTokenHasher.hash(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("hash-of-new-refresh");
        when(sessionRepository.updateRefreshTokenHash(eq(SESSION_ID), eq(REFRESH_TOKEN_HASH), eq("hash-of-new-refresh"), any(LocalDateTime.class)))
                .thenReturn(Mono.just(0L));
        when(sessionRepository.invalidateBySessionId(SESSION_ID)).thenReturn(Mono.just(1L));

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectError(RefreshTokenReuseDetectedException.class)
                .verify();

        verify(sessionRepository).invalidateBySessionId(SESSION_ID);
    }

    @Test
    @DisplayName("refresh token 재사용 탐지 — 동일 토큰으로 두 번 호출하면 두 번째에서 RefreshTokenReuseDetectedException이 발생한다")
    void refreshSecondCallWithSameTokenDetectsReuse() {
        Session validSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN_HASH, LocalDateTime.now().plusDays(14));

        // 첫 번째 호출 — 성공
        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(validSession));
        when(tokenGenerator.generateAccessToken(PRINCIPAL)).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(PRINCIPAL)).thenReturn("new-refresh");
        when(refreshTokenHasher.hash(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("hash-of-new-refresh");
        when(sessionRepository.updateRefreshTokenHash(eq(SESSION_ID), eq(REFRESH_TOKEN_HASH), eq("hash-of-new-refresh"), any(LocalDateTime.class)))
                .thenReturn(Mono.just(1L))    // 첫 번째 — 성공
                .thenReturn(Mono.just(0L));   // 두 번째 — 재사용 탐지
        when(sessionRepository.invalidateBySessionId(SESSION_ID)).thenReturn(Mono.just(1L));

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectError(RefreshTokenReuseDetectedException.class)
                .verify();

        verify(sessionRepository).invalidateBySessionId(SESSION_ID);
    }
}
