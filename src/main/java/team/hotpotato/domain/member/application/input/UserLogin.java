package team.hotpotato.domain.member.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.usecase.login.LoginCommand;
import team.hotpotato.domain.member.application.usecase.login.LoginResult;

public interface UserLogin {
    Mono<LoginResult> login(LoginCommand loginCommand);
}
