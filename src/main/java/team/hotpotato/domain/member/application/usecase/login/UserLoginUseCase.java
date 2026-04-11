package team.hotpotato.domain.member.application.usecase.login;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.input.UserLogin;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.domain.Session;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLoginUseCase implements UserLogin {
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;
    private final IdGenerator idGenerator;
    private final TransactionalOperator transactionalOperator;

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
                    String sessionId = UUID.randomUUID().toString();
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
                            LocalDateTime.now().plusSeconds(3600)
                    );

                    return sessionRepository.invalidateByUserId(user.id())
                            .then(sessionRepository.save(newSession))
                            .thenReturn(new LoginResult(accessToken, refreshToken));
                })
                .as(transactionalOperator::transactional);
    }

    private Mono<Boolean> isPasswordMatched(String rawPassword, String encodedPassword) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, encodedPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
