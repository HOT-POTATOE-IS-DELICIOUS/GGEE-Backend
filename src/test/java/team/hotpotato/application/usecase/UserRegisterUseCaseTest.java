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
import team.hotpotato.domain.member.application.event.ProtectTargetIndexingMessage;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;
import team.hotpotato.domain.member.application.output.UserAppender;
import team.hotpotato.domain.member.application.usecase.register.UserRegisterUseCase;
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
    private ProtectTargetIndexingPublisher protectTargetIndexingPublisher;

    @Mock
    private IdGenerator idGenerator;

    private PasswordEncoder passwordEncoder;
    private UserRegisterUseCase userRegisterUseCase;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userRegisterUseCase = new UserRegisterUseCase(userAppender, protectTargetIndexingPublisher, idGenerator, passwordEncoder);
    }

    @Test
    @DisplayName("회원가입 성공 시 사용자 저장과 비밀번호 인코딩이 수행된다")
    void registerCompletesAndStoresEncodedUser() {
        when(idGenerator.generateId()).thenReturn(100L);
        when(userAppender.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(protectTargetIndexingPublisher.publish(any(ProtectTargetIndexingMessage.class))).thenReturn(Mono.empty());

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword", "brand")))
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<ProtectTargetIndexingMessage> messageCaptor = ArgumentCaptor.forClass(ProtectTargetIndexingMessage.class);
        verify(idGenerator).generateId();
        verify(userAppender).save(userCaptor.capture());
        verify(protectTargetIndexingPublisher).publish(messageCaptor.capture());
        verifyNoMoreInteractions(idGenerator, userAppender, protectTargetIndexingPublisher);

        User savedUser = userCaptor.getValue();
        assertEquals(100L, savedUser.id());
        assertEquals("user@test.com", savedUser.email());
        assertEquals(Role.USER, savedUser.role());
        assertEquals("brand", savedUser.protectTarget());
        assertNotEquals("plainPassword", savedUser.password());
        assertTrue(passwordEncoder.matches("plainPassword", savedUser.password()));
        assertEquals(new ProtectTargetIndexingMessage("brand"), messageCaptor.getValue());
    }

    @Test
    @DisplayName("회원 저장 중 오류가 발생하면 예외를 그대로 전파한다")
    void registerPropagatesAppenderError() {
        RuntimeException expected = new RuntimeException("append failed");
        when(idGenerator.generateId()).thenReturn(1L);
        when(userAppender.save(any(User.class))).thenReturn(Mono.error(expected));

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword", "brand")))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(idGenerator).generateId();
        verify(userAppender).save(any(User.class));
        verifyNoInteractions(protectTargetIndexingPublisher);
        verifyNoMoreInteractions(idGenerator, userAppender);
    }

    @Test
    @DisplayName("인덱싱 이벤트 발행이 실패해도 회원가입은 완료된다")
    void registerCompletesWhenPublishFails() {
        when(idGenerator.generateId()).thenReturn(100L);
        when(userAppender.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(protectTargetIndexingPublisher.publish(any(ProtectTargetIndexingMessage.class)))
                .thenReturn(Mono.error(new RuntimeException("publish failed")));

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword", "brand")))
                .verifyComplete();

        verify(idGenerator).generateId();
        verify(userAppender).save(any(User.class));
        verify(protectTargetIndexingPublisher).publish(new ProtectTargetIndexingMessage("brand"));
        verifyNoMoreInteractions(idGenerator, userAppender, protectTargetIndexingPublisher);
    }
}
