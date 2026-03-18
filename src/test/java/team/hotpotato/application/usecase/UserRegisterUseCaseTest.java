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
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.dto.RegisterCommand;
import team.hotpotato.domain.member.application.persistence.UserAppender;
import team.hotpotato.domain.member.application.usecase.UserRegisterUseCase;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 유스케이스 단위 테스트")
class UserRegisterUseCaseTest {

    @Mock
    private UserAppender userAppender;

    @Mock
    private IdGenerator idGenerator;

    private PasswordEncoder passwordEncoder;
    private UserRegisterUseCase userRegisterUseCase;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("회원가입 성공 시 사용자 저장과 비밀번호 인코딩이 수행된다")
    void registerCompletesAndStoresEncodedUser() {
        when(idGenerator.generateId()).thenReturn(100L);
        when(userAppender.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword")))
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(idGenerator).generateId();
        verify(userAppender).save(userCaptor.capture());
        verifyNoMoreInteractions(idGenerator, userAppender);

        User savedUser = userCaptor.getValue();
        assertEquals(100L, savedUser.userId());
        assertEquals("user@test.com", savedUser.email());
        assertEquals(Role.USER, savedUser.role());
        assertNotEquals("plainPassword", savedUser.password());
        assertTrue(passwordEncoder.matches("plainPassword", savedUser.password()));
    }

    @Test
    @DisplayName("회원 저장 중 오류가 발생하면 예외를 그대로 전파한다")
    void registerPropagatesAppenderError() {
        RuntimeException expected = new RuntimeException("append failed");
        when(idGenerator.generateId()).thenReturn(1L);
        when(userAppender.save(any(User.class))).thenReturn(Mono.error(expected));

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword")))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(idGenerator).generateId();
        verify(userAppender).save(any(User.class));
        verifyNoMoreInteractions(idGenerator, userAppender);
    }

    @Test
    @DisplayName("이벤트 발행 중 오류가 발생하면 예외를 그대로 전파한다")
    void registerPropagatesPublisherError() {
        RuntimeException expected = new RuntimeException("publish failed");
        User savedUser = new User(3L, "user@test.com", passwordEncoder.encode("plainPassword"), Role.USER);

        when(idGenerator.generateId()).thenReturn(3L);
        when(userAppender.save(any(User.class))).thenReturn(Mono.just(savedUser));

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword")))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(idGenerator).generateId();
        verify(userAppender).save(any(User.class));
        verifyNoMoreInteractions(idGenerator, userAppender);
    }
}
