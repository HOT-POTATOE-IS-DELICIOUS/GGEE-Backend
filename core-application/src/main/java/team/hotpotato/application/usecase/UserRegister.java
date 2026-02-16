package team.hotpotato.application.usecase;

import reactor.core.publisher.Mono;
import team.hotpotato.application.dto.command.RegisterCommand;

public interface UserRegister {
    Mono<Void> register(RegisterCommand registerCommand);
}
