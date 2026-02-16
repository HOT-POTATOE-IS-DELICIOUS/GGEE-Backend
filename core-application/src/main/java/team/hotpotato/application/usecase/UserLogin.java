package team.hotpotato.application.usecase;

import reactor.core.publisher.Mono;
import team.hotpotato.application.dto.command.LoginCommand;
import team.hotpotato.application.dto.result.LoginResult;

public interface UserLogin {
    Mono<LoginResult> login(LoginCommand loginCommand);
}
