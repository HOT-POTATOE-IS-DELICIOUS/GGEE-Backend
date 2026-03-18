package team.hotpotato.domain.member.application.usecase;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.dto.LoginCommand;
import team.hotpotato.domain.member.application.dto.LoginResult;

public interface UserLogin {
    Mono<LoginResult> login(LoginCommand loginCommand);
}
