package team.hotpotato.domain.member.application.usecase.register;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.common.transaction.ReactiveTransactionRunner;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.PasswordHasher;
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

@RequiredArgsConstructor
public class UserRegisterUseCase implements UserRegister {
    private final UserRepository userRepository;
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final SessionRepository sessionRepository;
    private final TokenGenerator tokenGenerator;
    private final IdGenerator idGenerator;
    private final PasswordHasher passwordHasher;
    private final ReactiveTransactionRunner transactionRunner;
    private final long refreshTokenActiveTimeSeconds;

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
                .as(transactionRunner::transactional);
    }

    private Mono<String[]> createSession(User user) {
        String sessionId = String.valueOf(idGenerator.generateId());
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
        return passwordHasher.hash(registerCommand.password())
                .map(hashedPassword -> new User(
                        idGenerator.generateId(),
                        registerCommand.email(),
                        hashedPassword,
                        Role.USER,
                        registerCommand.protectTarget()
                ));
    }

    private Mono<ProtectTargetIndexingOutbox> saveOutbox(User savedUser) {
        return outboxRepository.save(new ProtectTargetIndexingOutbox(
                idGenerator.generateId(),
                savedUser.protectTarget(),
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        ));
    }

}
