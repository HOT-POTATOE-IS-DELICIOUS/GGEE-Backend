package team.hotpotato.domain.member.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.usecase.logout.LogoutCommand;

public interface UserLogout {
    Mono<Void> logout(LogoutCommand command);
}
