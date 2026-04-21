package team.hotpotato.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import team.hotpotato.GgeeBackendApplication;
import team.hotpotato.domain.member.application.input.TokenResolver;
import team.hotpotato.domain.member.application.input.UserTokenRefresh;
import team.hotpotato.domain.member.application.input.UserLogout;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.usecase.login.LoginCommand;
import team.hotpotato.domain.member.application.usecase.login.LoginResult;
import team.hotpotato.domain.member.application.usecase.login.InvalidSessionException;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;
import team.hotpotato.domain.member.application.usecase.logout.LogoutCommand;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshCommand;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshResult;
import team.hotpotato.domain.member.application.input.UserLogin;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;
import team.hotpotato.domain.member.infrastructure.jwt.ExpiredRefreshTokenException;
import team.hotpotato.domain.member.infrastructure.jwt.InvalidTokenException;
import team.hotpotato.domain.member.infrastructure.jwt.InvalidTokenTypeException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = GgeeBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("인증 API 통합 테스트")
class AuthControllerTest {

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @MockitoBean
    private UserRegister userRegister;

    @MockitoBean
    private UserLogin userLogin;

    @MockitoBean
    private UserTokenRefresh userTokenRefresh;

    @MockitoBean
    private UserLogout userLogout;

    @MockitoBean
    private TokenResolver tokenResolver;

    @MockitoBean
    private SessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @DisplayName("회원가입 요청은 command로 변환되어 201을 반환한다")
    void registerReturnsCreated() {
        when(userRegister.register(any(RegisterCommand.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "email": "user@test.com",
                          "password": "plainPassword",
                          "protectTarget": "brand",
                          "protectTargetInfo": "브랜드 공식몰"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        ArgumentCaptor<RegisterCommand> commandCaptor = ArgumentCaptor.forClass(RegisterCommand.class);
        verify(userRegister).register(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(
                new RegisterCommand("user@test.com", "plainPassword", "brand", "브랜드 공식몰")
        );
    }

    @Test
    @DisplayName("로그인 요청은 토큰 응답을 반환한다")
    void loginReturnsTokens() {
        when(userLogin.login(any(LoginCommand.class)))
                .thenReturn(Mono.just(new LoginResult("access-token", "refresh-token")));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "email": "user@test.com",
                          "password": "plainPassword"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access-token")
                .jsonPath("$.refreshToken").isEqualTo("refresh-token");

        ArgumentCaptor<LoginCommand> commandCaptor = ArgumentCaptor.forClass(LoginCommand.class);
        verify(userLogin).login(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new LoginCommand("user@test.com", "plainPassword"));
    }

    @Test
    @DisplayName("토큰 갱신 요청은 새 토큰 응답을 반환한다")
    void refreshReturnsTokens() {
        when(userTokenRefresh.refresh(any(RefreshCommand.class)))
                .thenReturn(Mono.just(new RefreshResult("new-access-token", "new-refresh-token")));

        webTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "refreshToken": "valid-refresh-token"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("new-access-token")
                .jsonPath("$.refreshToken").isEqualTo("new-refresh-token");

        ArgumentCaptor<RefreshCommand> commandCaptor = ArgumentCaptor.forClass(RefreshCommand.class);
        verify(userTokenRefresh).refresh(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new RefreshCommand("valid-refresh-token"));
    }

    @Test
    @DisplayName("만료된 refresh token으로 갱신 요청하면 401을 반환한다")
    void refreshReturnsUnauthorizedWhenRefreshTokenExpired() {
        when(userTokenRefresh.refresh(any(RefreshCommand.class)))
                .thenReturn(Mono.error(ExpiredRefreshTokenException.EXCEPTION));

        webTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "refreshToken": "expired-refresh-token"
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class).isEqualTo("만료된 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("access token을 refresh 엔드포인트에 전달하면 400을 반환한다")
    void refreshReturnsBadRequestWhenAccessTokenIsProvided() {
        when(userTokenRefresh.refresh(any(RefreshCommand.class)))
                .thenReturn(Mono.error(InvalidTokenTypeException.EXCEPTION));

        webTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "refreshToken": "access-token"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("올바르지 않은 JWT 토큰 타입입니다.");
    }

    @Test
    @DisplayName("변조된 JWT로 갱신 요청하면 400을 반환한다")
    void refreshReturnsBadRequestWhenJwtIsTampered() {
        when(userTokenRefresh.refresh(any(RefreshCommand.class)))
                .thenReturn(Mono.error(InvalidTokenException.EXCEPTION));

        webTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "refreshToken": "tampered.jwt.token"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("올바르지 않은 JWT 토큰입니다.");
    }

    @Test
    @DisplayName("세션이 무효화된 refresh token으로 갱신 요청하면 401을 반환한다")
    void refreshReturnsUnauthorizedWhenSessionIsInvalidated() {
        when(userTokenRefresh.refresh(any(RefreshCommand.class)))
                .thenReturn(Mono.error(InvalidSessionException.EXCEPTION));

        webTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "refreshToken": "invalidated-refresh-token"
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class).isEqualTo("유효하지 않은 세션입니다.");
    }

    @Test
    @DisplayName("로그아웃 요청은 인증된 사용자 세션을 무효화하고 204를 반환한다")
    void logoutReturnsNoContent() {
        String sessionId = "logout-session-id";
        when(tokenResolver.resolve("Bearer valid-access-token"))
                .thenReturn(Mono.just(new AuthPrincipal(7L, Role.USER, sessionId)));
        when(sessionRepository.findActiveByUserId(7L))
                .thenReturn(Mono.just(new Session(1L, 7L, sessionId, "refresh-token", LocalDateTime.now().plusHours(1))));
        when(userLogout.logout(any(LogoutCommand.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer valid-access-token")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        ArgumentCaptor<LogoutCommand> commandCaptor = ArgumentCaptor.forClass(LogoutCommand.class);
        verify(userLogout).logout(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new LogoutCommand(7L));
    }

    @Test
    @DisplayName("로그아웃 요청은 인증 정보가 없으면 401을 반환한다")
    void logoutReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("잘못된 요청 본문은 400을 반환한다")
    void registerRejectsInvalidRequest() {
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "email": "invalid-email",
                          "password": "short",
                          "protectTarget": "",
                          "protectTargetInfo": ""
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("공개 경로는 잘못된 Authorization 헤더가 있어도 요청을 통과시킨다")
    void publicRouteIgnoresInvalidAuthorizationHeader() {
        when(tokenResolver.resolve(any())).thenReturn(Mono.error(InvalidTokenException.EXCEPTION));
        when(userLogin.login(any(LoginCommand.class)))
                .thenReturn(Mono.just(new LoginResult("access-token", "refresh-token")));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer invalid.token.value")
                .bodyValue("""
                        {
                          "email": "user@test.com",
                          "password": "plainPassword"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access-token")
                .jsonPath("$.refreshToken").isEqualTo("refresh-token");
    }
}
