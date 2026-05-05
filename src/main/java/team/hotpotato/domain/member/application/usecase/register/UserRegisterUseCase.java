package team.hotpotato.domain.member.application.usecase.register;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.infrastructure.jwt.TokenProperties;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.common.transaction.ReactiveTransactionRunner;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.PasswordHasher;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;
import team.hotpotato.domain.member.domain.User;
import team.hotpotato.domain.protect.application.input.IndexProtect;
import team.hotpotato.domain.protect.application.usecase.indexing.IndexProtectCommand;
import team.hotpotato.domain.protect.application.usecase.indexing.IndexProtectResult;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserRegisterUseCase implements UserRegister {
    private final UserRepository userRepository;
    private final IndexProtect indexProtect;
    private final SessionRepository sessionRepository;
    private final TokenGenerator tokenGenerator;
    private final IdGenerator idGenerator;
    private final PasswordHasher passwordHasher;
    private final ReactiveTransactionRunner transactionRunner;
    private final TokenProperties tokenProperties;

    @Override
    public Mono<RegisterResult> register(RegisterCommand registerCommand) {
        return createUser(registerCommand)
                .flatMap(user -> userRepository.save(user)
                        .flatMap(savedUser -> indexProtect.index(
                                        new IndexProtectCommand(
                                                savedUser.id(),
                                                registerCommand.protectTarget(),
                                                registerCommand.protectTargetInfo()
                                        )
                                )
                                .map(protectResult -> new PersistedRegistration(savedUser, protectResult))
                        )
                )
                .as(transactionRunner::transactional)
                .flatMap(persisted -> createSession(persisted.user())
                        .map(tokens -> new RegisterResult(
                                String.valueOf(persisted.protectResult().indexingJobId()),
                                tokens[0],
                                tokens[1]
                        ))
                );
    }

    private record PersistedRegistration(User user, IndexProtectResult protectResult) {
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
                LocalDateTime.now().plusSeconds(tokenProperties.refreshTokenActiveTime())
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
                        Role.USER
                ));
    }
}
