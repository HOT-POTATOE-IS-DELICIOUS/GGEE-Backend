package team.hotpotato.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.input.RefreshTokenResolver;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.usecase.login.InvalidSessionException;
import team.hotpotato.domain.member.application.usecase.login.SessionExpiredException;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshCommand;
import team.hotpotato.domain.member.application.usecase.refresh.UserTokenRefreshUseCase;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;

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

    private UserTokenRefreshUseCase useCase;

    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String SESSION_ID = "session-id";
    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(1L, Role.USER, SESSION_ID);

    @BeforeEach
    void setUp() {
        useCase = new UserTokenRefreshUseCase(refreshTokenResolver, sessionRepository, tokenGenerator);
        ReflectionTestUtils.setField(useCase, "refreshTokenActiveTimeSeconds", 1209600L);
    }

    @Test
    @DisplayName("유효한 refresh token과 세션이면 새 토큰 쌍을 반환한다")
    void refreshReturnsNewTokensWhenSessionIsValid() {
        Session validSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN, LocalDateTime.now().plusDays(14));

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(validSession));
        when(tokenGenerator.generateAccessToken(PRINCIPAL)).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(PRINCIPAL)).thenReturn("new-refresh");
        when(sessionRepository.updateRefreshToken(eq(SESSION_ID), eq("new-refresh"), any(LocalDateTime.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .assertNext(result -> {
                    assertEquals("new-access", result.accessToken());
                    assertEquals("new-refresh", result.refreshToken());
                })
                .verifyComplete();

        verify(sessionRepository).updateRefreshToken(eq(SESSION_ID), eq("new-refresh"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("updateRefreshToken에 갱신된 만료 시간이 전달된다")
    void refreshPassesNewExpiresAtToRepository() {
        Session validSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN, LocalDateTime.now().plusDays(14));
        ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(validSession));
        when(tokenGenerator.generateAccessToken(any())).thenReturn("new-access");
        when(tokenGenerator.generateRefreshToken(any())).thenReturn("new-refresh");
        when(sessionRepository.updateRefreshToken(any(), any(), expiresAtCaptor.capture())).thenReturn(Mono.empty());

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
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectError(InvalidSessionException.class)
                .verify();

        verifyNoInteractions(tokenGenerator);
    }

    @Test
    @DisplayName("세션이 만료됐으면 SessionExpiredException이 발생한다")
    void refreshFailsWhenSessionExpired() {
        Session expiredSession = new Session(1L, 1L, SESSION_ID, REFRESH_TOKEN, LocalDateTime.now().minusSeconds(1));

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(expiredSession));

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectError(SessionExpiredException.class)
                .verify();

        verifyNoInteractions(tokenGenerator);
    }

    @Test
    @DisplayName("DB의 refresh token과 일치하지 않으면 InvalidSessionException이 발생한다")
    void refreshFailsWhenRefreshTokenMismatch() {
        Session sessionWithDifferentToken = new Session(1L, 1L, SESSION_ID, "other-token", LocalDateTime.now().plusDays(14));

        when(refreshTokenResolver.resolve(REFRESH_TOKEN)).thenReturn(Mono.just(PRINCIPAL));
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Mono.just(sessionWithDifferentToken));

        StepVerifier.create(useCase.refresh(new RefreshCommand(REFRESH_TOKEN)))
                .expectError(InvalidSessionException.class)
                .verify();

        verifyNoInteractions(tokenGenerator);
    }
}
