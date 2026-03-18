package team.hotpotato.domain.member.application.usecase;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.dto.RegisterCommand;

public interface UserRegister {
    Mono<Void> register(RegisterCommand registerCommand);
}
