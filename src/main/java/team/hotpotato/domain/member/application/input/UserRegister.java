package team.hotpotato.domain.member.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;
import team.hotpotato.domain.member.application.usecase.register.RegisterResult;

public interface UserRegister {
    Mono<RegisterResult> register(RegisterCommand registerCommand);
}
