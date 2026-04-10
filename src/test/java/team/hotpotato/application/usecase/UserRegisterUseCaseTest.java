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
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxAppender;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;
import team.hotpotato.domain.member.application.output.UserAppender;
import team.hotpotato.domain.member.application.usecase.register.UserRegisterUseCase;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;
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
    private ProtectTargetIndexingOutboxAppender outboxAppender;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TransactionalOperator transactionalOperator;

    private PasswordEncoder passwordEncoder;
    private UserRegisterUseCase userRegisterUseCase;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userRegisterUseCase = new UserRegisterUseCase(userAppender, outboxAppender, idGenerator, passwordEncoder, transactionalOperator);
        lenient().when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("회원가입 성공 시 사용자 저장과 outbox 적재가 함께 수행된다")
    void registerCompletesAndStoresEncodedUser() {
        when(idGenerator.generateId()).thenReturn(100L, 200L);
        when(userAppender.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(outboxAppender.save(any(ProtectTargetIndexingOutbox.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword", "brand")))
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<ProtectTargetIndexingOutbox> outboxCaptor = ArgumentCaptor.forClass(ProtectTargetIndexingOutbox.class);
        verify(idGenerator, times(2)).generateId();
        verify(userAppender).save(userCaptor.capture());
        verify(outboxAppender).save(outboxCaptor.capture());
        verifyNoMoreInteractions(idGenerator, userAppender, outboxAppender);

        User savedUser = userCaptor.getValue();
        assertEquals(100L, savedUser.id());
        assertEquals("user@test.com", savedUser.email());
        assertEquals(Role.USER, savedUser.role());
        assertEquals("brand", savedUser.protectTarget());
        assertNotEquals("plainPassword", savedUser.password());
        assertTrue(passwordEncoder.matches("plainPassword", savedUser.password()));

        ProtectTargetIndexingOutbox savedOutbox = outboxCaptor.getValue();
        assertEquals(200L, savedOutbox.id());
        assertEquals("brand", savedOutbox.protectTarget());
        assertEquals(ProtectTargetIndexingOutboxStatus.PENDING, savedOutbox.status());
        assertNull(savedOutbox.publishedAt());
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
        verifyNoInteractions(outboxAppender);
        verifyNoMoreInteractions(idGenerator, userAppender);
    }

    @Test
    @DisplayName("outbox 저장이 실패하면 회원가입도 실패한다")
    void registerFailsWhenOutboxSaveFails() {
        RuntimeException expected = new RuntimeException("outbox failed");
        when(idGenerator.generateId()).thenReturn(100L, 200L);
        when(userAppender.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(outboxAppender.save(any(ProtectTargetIndexingOutbox.class)))
                .thenReturn(Mono.error(expected));

        StepVerifier.create(userRegisterUseCase.register(new RegisterCommand("user@test.com", "plainPassword", "brand")))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(idGenerator, times(2)).generateId();
        verify(userAppender).save(any(User.class));
        verify(outboxAppender).save(any(ProtectTargetIndexingOutbox.class));
        verifyNoMoreInteractions(idGenerator, userAppender, outboxAppender);
    }
}
