package team.hotpotato.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.usecase.login.LoginCommand;
import team.hotpotato.domain.member.application.output.UserReader;
import team.hotpotato.domain.member.application.usecase.login.InvalidEmailOrPasswordException;
import team.hotpotato.domain.member.application.usecase.login.UserLoginUseCase;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("로그인 유스케이스 단위 테스트")
class UserLoginUseCaseTest {

    @Mock
    private UserReader userReader;

    @Mock
    private TokenGenerator tokenGenerator;

    private PasswordEncoder passwordEncoder;
    private UserLoginUseCase userLoginUseCase;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userLoginUseCase = new UserLoginUseCase(userReader, tokenGenerator, passwordEncoder);
    }

    @Test
    @DisplayName("이메일/비밀번호가 일치하면 액세스/리프레시 토큰을 반환한다")
    void loginReturnsTokenPairWhenCredentialsMatch() {
        User user = new User(7L, "login@test.com", passwordEncoder.encode("password123"), Role.USER);
        when(userReader.findByEmail("login@test.com")).thenReturn(Mono.just(user));
        when(tokenGenerator.generateAccessToken(any(AuthPrincipal.class))).thenReturn("access-7-USER");
        when(tokenGenerator.generateRefreshToken(any(AuthPrincipal.class))).thenReturn("refresh-7-USER");

        StepVerifier.create(userLoginUseCase.login(new LoginCommand("login@test.com", "password123")))
                .assertNext(result -> {
                    assertEquals("access-7-USER", result.accessToken());
                    assertEquals("refresh-7-USER", result.refreshToken());
                })
                .verifyComplete();

        verify(userReader).findByEmail("login@test.com");
        verify(tokenGenerator).generateAccessToken(any(AuthPrincipal.class));
        verify(tokenGenerator).generateRefreshToken(any(AuthPrincipal.class));
        verifyNoMoreInteractions(userReader, tokenGenerator);
    }

    @Test
    @DisplayName("토큰 생성에는 도메인 Role이 전달된다")
    void loginBuildsAuthPrincipalWithDomainRole() {
        User user = new User(9L, "role@test.com", passwordEncoder.encode("password123"), Role.ADMIN);
        ArgumentCaptor<AuthPrincipal> principalCaptor = ArgumentCaptor.forClass(AuthPrincipal.class);

        when(userReader.findByEmail("role@test.com")).thenReturn(Mono.just(user));
        when(tokenGenerator.generateAccessToken(principalCaptor.capture())).thenReturn("access");
        when(tokenGenerator.generateRefreshToken(any(AuthPrincipal.class))).thenReturn("refresh");

        StepVerifier.create(userLoginUseCase.login(new LoginCommand("role@test.com", "password123")))
                .expectNextCount(1)
                .verifyComplete();

        assertEquals(Role.ADMIN, principalCaptor.getValue().role());
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 InvalidEmailOrPasswordException이 발생한다")
    void loginFailsWhenEmailDoesNotExist() {
        when(userReader.findByEmail("missing@test.com")).thenReturn(Mono.empty());

        StepVerifier.create(userLoginUseCase.login(new LoginCommand("missing@test.com", "password123")))
                .expectError(InvalidEmailOrPasswordException.class)
                .verify();

        verify(userReader).findByEmail("missing@test.com");
        verifyNoInteractions(tokenGenerator);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 InvalidEmailOrPasswordException이 발생한다")
    void loginFailsWhenPasswordDoesNotMatch() {
        User user = new User(7L, "login@test.com", passwordEncoder.encode("password123"), Role.USER);
        when(userReader.findByEmail("login@test.com")).thenReturn(Mono.just(user));

        StepVerifier.create(userLoginUseCase.login(new LoginCommand("login@test.com", "wrongPassword")))
                .expectError(InvalidEmailOrPasswordException.class)
                .verify();

        verify(userReader).findByEmail("login@test.com");
        verifyNoInteractions(tokenGenerator);
    }
}
