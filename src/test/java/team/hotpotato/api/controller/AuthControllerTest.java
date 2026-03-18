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
import team.hotpotato.domain.member.application.auth.TokenResolver;
import team.hotpotato.domain.member.application.dto.LoginCommand;
import team.hotpotato.domain.member.application.dto.LoginResult;
import team.hotpotato.domain.member.application.dto.RegisterCommand;
import team.hotpotato.domain.member.application.usecase.UserLogin;
import team.hotpotato.domain.member.application.usecase.UserRegister;

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
    private TokenResolver tokenResolver;

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
                          "password": "plainPassword"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        ArgumentCaptor<RegisterCommand> commandCaptor = ArgumentCaptor.forClass(RegisterCommand.class);
        verify(userRegister).register(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new RegisterCommand("user@test.com", "plainPassword"));
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
    @DisplayName("잘못된 요청 본문은 400을 반환한다")
    void registerRejectsInvalidRequest() {
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "email": "invalid-email",
                          "password": "short"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
