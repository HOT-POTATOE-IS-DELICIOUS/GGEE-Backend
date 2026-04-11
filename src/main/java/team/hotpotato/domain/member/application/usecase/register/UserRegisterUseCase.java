package team.hotpotato.domain.member.application.usecase.register;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;
import team.hotpotato.domain.member.domain.User;
import team.hotpotato.domain.member.application.usecase.register.RegisterResult;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRegisterUseCase implements UserRegister {
    private final UserRepository userRepository;
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final SessionRepository sessionRepository;
    private final TokenGenerator tokenGenerator;
    private final IdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalOperator transactionalOperator;

    @Value("${jwt.refresh-token-active-time}")
    private long refreshTokenActiveTimeSeconds;

    @Override
    public Mono<RegisterResult> register(RegisterCommand registerCommand) {
        return createUser(registerCommand)
                .flatMap(user -> userRepository.save(user)
                        .flatMap(savedUser -> saveOutbox(savedUser)
                                .flatMap(outbox -> createSession(savedUser)
                                        .map(tokens -> new RegisterResult(
                                                String.valueOf(outbox.id()),
                                                tokens[0],
                                                tokens[1]
                                        ))
                                )
                        )
                )
                .as(transactionalOperator::transactional)
                .onErrorMap(e -> {
                    if (isDuplicateEmailError(e)) {
                        return EmailAlreadyExistsException.EXCEPTION;
                    }
                    return e;
                });
    }

    private Mono<String[]> createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        AuthPrincipal authPrincipal = new AuthPrincipal(user.id(), user.role(), sessionId);

        String accessToken = tokenGenerator.generateAccessToken(authPrincipal);
        String refreshToken = tokenGenerator.generateRefreshToken(authPrincipal);

        Session session = new Session(
                idGenerator.generateId(),
                user.id(),
                sessionId,
                refreshToken,
                LocalDateTime.now().plusSeconds(refreshTokenActiveTimeSeconds)
        );

        return sessionRepository.save(session)
                .thenReturn(new String[]{accessToken, refreshToken});
    }

    private Mono<User> createUser(RegisterCommand registerCommand) {
        return Mono.fromCallable(() -> new User(
                        idGenerator.generateId(),
                        registerCommand.email(),
                        passwordEncoder.encode(registerCommand.password()),
                        Role.USER,
                        registerCommand.protectTarget()
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<ProtectTargetIndexingOutbox> saveOutbox(User savedUser) {
        return outboxRepository.save(new ProtectTargetIndexingOutbox(
                idGenerator.generateId(),
                savedUser.protectTarget(),
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        ));
    }

    private boolean isDuplicateEmailError(Throwable e) {
        if (e instanceof DataIntegrityViolationException) {
            String message = e.getMessage();
            return message != null && message.contains("email");
        }
        return false;
    }
}
