package team.hotpotato.domain.member.application.usecase.login;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import team.hotpotato.common.transaction.ReactiveTransactionRunner;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.PasswordHasher;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.input.UserLogin;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.domain.Session;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class UserLoginUseCase implements UserLogin {
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final IdGenerator idGenerator;
    private final ReactiveTransactionRunner transactionRunner;
    private final long refreshTokenActiveTimeSeconds;

    @Override
    public Mono<LoginResult> login(LoginCommand loginCommand) {
        return userRepository.findByEmail(loginCommand.email())
                .switchIfEmpty(Mono.error(InvalidEmailOrPasswordException.EXCEPTION))
                .flatMap(user -> isPasswordMatched(loginCommand.password(), user.password())
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(InvalidEmailOrPasswordException.EXCEPTION))
                        .thenReturn(user)
                )
                .flatMap(user -> {
                    String sessionId = String.valueOf(idGenerator.generateId());
                    AuthPrincipal authPrincipal = new AuthPrincipal(
                            user.id(),
                            user.role(),
                            sessionId
                    );

                    String accessToken = tokenGenerator.generateAccessToken(authPrincipal);
                    String refreshToken = tokenGenerator.generateRefreshToken(authPrincipal);

                    Session newSession = new Session(
                            idGenerator.generateId(),
                            user.id(),
                            sessionId,
                            refreshToken,
                            LocalDateTime.now().plusSeconds(refreshTokenActiveTimeSeconds)
                    );

                    return sessionRepository.invalidateByUserId(user.id())
                            .then(sessionRepository.save(newSession))
                            .thenReturn(new LoginResult(accessToken, refreshToken));
                })
                .as(transactionRunner::transactional);
    }

    private Mono<Boolean> isPasswordMatched(String rawPassword, String encodedPassword) {
        return passwordHasher.matches(rawPassword, encodedPassword);
    }
}
