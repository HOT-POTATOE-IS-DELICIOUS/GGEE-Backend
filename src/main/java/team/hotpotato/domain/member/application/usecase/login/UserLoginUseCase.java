package team.hotpotato.domain.member.application.usecase.login;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.input.UserLogin;
import team.hotpotato.domain.member.application.output.UserReader;

@Service
@RequiredArgsConstructor
public class UserLoginUseCase implements UserLogin {
    private final UserReader userReader;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<LoginResult> login(LoginCommand loginCommand) {
        return userReader.findByEmail(loginCommand.email())
                .switchIfEmpty(Mono.error(InvalidEmailOrPasswordException.EXCEPTION))
                .flatMap(user -> isPasswordMatched(loginCommand.password(), user.password())
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(InvalidEmailOrPasswordException.EXCEPTION))
                        .thenReturn(user)
                )
                .map(user -> {
                    AuthPrincipal authPrincipal = new AuthPrincipal(
                            user.id(),
                            user.role()
                    );

                    return new LoginResult(
                            tokenGenerator.generateAccessToken(authPrincipal),
                            tokenGenerator.generateRefreshToken(authPrincipal)
                    );
                });
    }

    private Mono<Boolean> isPasswordMatched(String rawPassword, String encodedPassword) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, encodedPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
