package team.hotpotato.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.common.transaction.ReactiveTransactionRunner;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.PasswordHasher;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;
import team.hotpotato.domain.member.application.usecase.register.RegisterResult;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.application.usecase.register.UserRegisterUseCase;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;
import team.hotpotato.domain.member.domain.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 유스케이스 단위 테스트")
class UserRegisterUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProtectTargetIndexingOutboxRepository outboxRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private IdGenerator idGenerator;

    private BCryptPasswordEncoder passwordEncoder;
    private PasswordHasher passwordHasher;
    private ReactiveTransactionRunner transactionRunner;
    private UserRegisterUseCase userRegisterUseCase;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        passwordHasher = new PasswordHasher() {
            @Override
            public Mono<String> hash(String rawPassword) {
                return Mono.just(passwordEncoder.encode(rawPassword));
            }

            @Override
            public Mono<Boolean> matches(String rawPassword, String hashedPassword) {
                return Mono.just(passwordEncoder.matches(rawPassword, hashedPassword));
            }
        };
        transactionRunner = new ReactiveTransactionRunner() {
            @Override
            public <T> Mono<T> transactional(Mono<T> mono) {
                return mono;
            }
        };
        userRegisterUseCase = new UserRegisterUseCase(
                userRepository,
                outboxRepository,
                sessionRepository,
                tokenGenerator,
                idGenerator,
                passwordHasher,
                transactionRunner,
                1_209_600L
        );
    }

    @Test
    @DisplayName("회원가입 성공 시 사용자 저장과 outbox 적재가 함께 수행된다")
    void registerCompletesAndStoresEncodedUser() {
        when(idGenerator.generateId()).thenReturn(100L, 200L, 300L, 400L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(outboxRepository.save(any(ProtectTargetIndexingOutbox.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(tokenGenerator.generateAccessToken(any(AuthPrincipal.class))).thenReturn("access-token");
        when(tokenGenerator.generateRefreshToken(any(AuthPrincipal.class))).thenReturn("refresh-token");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userRegisterUseCase.register(
                        new RegisterCommand("user@test.com", "plainPassword", "brand", "브랜드 공식몰")
                ))
                .expectNext(new RegisterResult("200", "access-token", "refresh-token"))
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<ProtectTargetIndexingOutbox> outboxCaptor = ArgumentCaptor.forClass(ProtectTargetIndexingOutbox.class);
        verify(idGenerator, times(4)).generateId();
        verify(userRepository).save(userCaptor.capture());
        verify(outboxRepository).save(outboxCaptor.capture());
        verifyNoMoreInteractions(idGenerator, userRepository, outboxRepository);

        User savedUser = userCaptor.getValue();
        assertEquals(100L, savedUser.id());
        assertEquals("user@test.com", savedUser.email());
        assertEquals(Role.USER, savedUser.role());
        assertEquals("brand", savedUser.protectTarget());
        assertEquals("브랜드 공식몰", savedUser.protectTargetInfo());
        assertNotEquals("plainPassword", savedUser.password());
        assertTrue(passwordEncoder.matches("plainPassword", savedUser.password()));

        ProtectTargetIndexingOutbox savedOutbox = outboxCaptor.getValue();
        assertEquals(200L, savedOutbox.id());
        assertEquals("brand", savedOutbox.protectTarget());
        assertEquals("브랜드 공식몰", savedOutbox.protectTargetInfo());
        assertEquals(ProtectTargetIndexingOutboxStatus.PENDING, savedOutbox.status());
        assertNull(savedOutbox.publishedAt());
    }

    @Test
    @DisplayName("회원 저장 중 오류가 발생하면 예외를 그대로 전파한다")
    void registerPropagatesAppenderError() {
        RuntimeException expected = new RuntimeException("append failed");
        when(idGenerator.generateId()).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenReturn(Mono.error(expected));

        StepVerifier.create(userRegisterUseCase.register(
                        new RegisterCommand("user@test.com", "plainPassword", "brand", "브랜드 공식몰")
                ))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(idGenerator).generateId();
        verify(userRepository).save(any(User.class));
        verifyNoInteractions(outboxRepository);
        verifyNoMoreInteractions(idGenerator, userRepository);
    }

    @Test
    @DisplayName("outbox 저장이 실패하면 회원가입도 실패한다")
    void registerFailsWhenOutboxSaveFails() {
        RuntimeException expected = new RuntimeException("outbox failed");
        when(idGenerator.generateId()).thenReturn(100L, 200L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(outboxRepository.save(any(ProtectTargetIndexingOutbox.class)))
                .thenReturn(Mono.error(expected));

        StepVerifier.create(userRegisterUseCase.register(
                        new RegisterCommand("user@test.com", "plainPassword", "brand", "브랜드 공식몰")
                ))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(idGenerator, times(2)).generateId();
        verify(userRepository).save(any(User.class));
        verify(outboxRepository).save(any(ProtectTargetIndexingOutbox.class));
        verifyNoMoreInteractions(idGenerator, userRepository, outboxRepository);
    }
}
