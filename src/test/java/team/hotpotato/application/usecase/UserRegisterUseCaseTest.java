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
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;
import team.hotpotato.domain.member.application.usecase.register.RegisterResult;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.application.usecase.register.UserRegisterUseCase;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;
import team.hotpotato.domain.member.domain.User;
import team.hotpotato.domain.member.infrastructure.jwt.TokenProperties;
import team.hotpotato.domain.protect.application.input.IndexProtect;
import team.hotpotato.domain.protect.application.usecase.indexing.IndexProtectCommand;
import team.hotpotato.domain.protect.application.usecase.indexing.IndexProtectResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 유스케이스 단위 테스트")
class UserRegisterUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IndexProtect indexProtect;

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
                indexProtect,
                sessionRepository,
                tokenGenerator,
                idGenerator,
                passwordHasher,
                transactionRunner,
                new TokenProperties(3600L, 1_209_600L, "Bearer", "Authorization", "dummyKey")
        );
    }

    @Test
    @DisplayName("회원가입 성공 시 사용자 저장과 protect 등록이 함께 수행된다")
    void registerCompletesAndStoresEncodedUser() {
        when(idGenerator.generateId()).thenReturn(100L, 300L, 400L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(indexProtect.index(any(IndexProtectCommand.class)))
                .thenReturn(Mono.just(new IndexProtectResult(201L, 200L)));
        when(tokenGenerator.generateAccessToken(any(AuthPrincipal.class))).thenReturn("access-token");
        when(tokenGenerator.generateRefreshToken(any(AuthPrincipal.class))).thenReturn("refresh-token");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userRegisterUseCase.register(
                        new RegisterCommand("user@test.com", "plainPassword", "brand", "브랜드 공식몰")
                ))
                .expectNext(new RegisterResult("200", "access-token", "refresh-token"))
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<IndexProtectCommand> protectCaptor = ArgumentCaptor.forClass(IndexProtectCommand.class);
        verify(userRepository).save(userCaptor.capture());
        verify(indexProtect).index(protectCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(100L, savedUser.id());
        assertEquals("user@test.com", savedUser.email());
        assertEquals(Role.USER, savedUser.role());
        assertNotEquals("plainPassword", savedUser.password());
        assertTrue(passwordEncoder.matches("plainPassword", savedUser.password()));

        IndexProtectCommand protectCommand = protectCaptor.getValue();
        assertEquals(100L, protectCommand.userId());
        assertEquals("brand", protectCommand.target());
        assertEquals("브랜드 공식몰", protectCommand.info());
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
        verifyNoInteractions(indexProtect);
    }

    @Test
    @DisplayName("protect 등록이 실패하면 회원가입도 실패하고 세션은 생성되지 않는다")
    void registerFailsWhenProtectRegistrationFails() {
        RuntimeException expected = new RuntimeException("protect failed");
        when(idGenerator.generateId()).thenReturn(100L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(indexProtect.index(any(IndexProtectCommand.class)))
                .thenReturn(Mono.error(expected));

        StepVerifier.create(userRegisterUseCase.register(
                        new RegisterCommand("user@test.com", "plainPassword", "brand", "brand info")
                ))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(userRepository).save(any(User.class));
        verify(indexProtect).index(any(IndexProtectCommand.class));
        verifyNoInteractions(sessionRepository);
    }

    @Test
    @DisplayName("세션 생성은 트랜잭션 밖에서 수행되어, 실패해도 user/protect 적재 후 호출된다")
    void sessionCreationRunsAfterTransactionCommits() {
        RuntimeException sessionFailure = new RuntimeException("session save failed");
        when(idGenerator.generateId()).thenReturn(100L, 300L, 400L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(indexProtect.index(any(IndexProtectCommand.class)))
                .thenReturn(Mono.just(new IndexProtectResult(201L, 200L)));
        when(tokenGenerator.generateAccessToken(any(AuthPrincipal.class))).thenReturn("access-token");
        when(tokenGenerator.generateRefreshToken(any(AuthPrincipal.class))).thenReturn("refresh-token");
        when(sessionRepository.save(any(Session.class))).thenReturn(Mono.error(sessionFailure));

        StepVerifier.create(userRegisterUseCase.register(
                        new RegisterCommand("user@test.com", "plainPassword", "brand", "brand info")
                ))
                .expectErrorMatches(error -> error == sessionFailure)
                .verify();

        verify(userRepository).save(any(User.class));
        verify(indexProtect).index(any(IndexProtectCommand.class));
        verify(sessionRepository).save(any(Session.class));
    }
}
