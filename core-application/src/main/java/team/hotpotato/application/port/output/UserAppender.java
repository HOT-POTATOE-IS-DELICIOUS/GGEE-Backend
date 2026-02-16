package team.hotpotato.application.port.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.user.User;

public interface UserAppender {
    Mono<User> save(User user);
}
