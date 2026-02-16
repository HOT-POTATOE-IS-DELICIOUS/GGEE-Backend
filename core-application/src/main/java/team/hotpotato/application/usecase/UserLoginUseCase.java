package team.hotpotato.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.application.dto.command.LoginCommand;
import team.hotpotato.application.dto.model.AuthPrincipal;
import team.hotpotato.application.dto.result.LoginResult;
import team.hotpotato.application.exception.InvalidEmailOrPasswordException;
import team.hotpotato.application.port.output.TokenGenerator;
import team.hotpotato.application.port.output.UserReader;

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
                .filter(user -> passwordEncoder.matches(loginCommand.password(), user.password()))
                .switchIfEmpty(Mono.error(InvalidEmailOrPasswordException.EXCEPTION))
                .map(user -> {
                    AuthPrincipal authPrincipal = new AuthPrincipal(
                            user.userId(),
                            user.role().name()
                    );

                    return new LoginResult(
                            tokenGenerator.generateAccessToken(authPrincipal),
                            tokenGenerator.generateRefreshToken(authPrincipal)
                    );
                });
    }
}
