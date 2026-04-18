package team.hotpotato.domain.member.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.User;

public interface GetUser {
    Mono<User> get(Long userId);
}
