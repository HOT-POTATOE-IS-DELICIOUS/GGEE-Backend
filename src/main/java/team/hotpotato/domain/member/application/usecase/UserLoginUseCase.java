package team.hotpotato.domain.member.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.auth.AuthPrincipal;
import team.hotpotato.domain.member.application.auth.TokenGenerator;
import team.hotpotato.domain.member.application.dto.LoginCommand;
import team.hotpotato.domain.member.application.dto.LoginResult;
import team.hotpotato.domain.member.application.persistence.UserReader;

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
                            user.id(),
                            user.role().name()
                    );

                    return new LoginResult(
                            tokenGenerator.generateAccessToken(authPrincipal),
                            tokenGenerator.generateRefreshToken(authPrincipal)
                    );
                });
    }
}
