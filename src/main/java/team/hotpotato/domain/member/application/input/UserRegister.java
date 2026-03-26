package team.hotpotato.domain.member.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;

public interface UserRegister {
    Mono<Void> register(RegisterCommand registerCommand);
}
