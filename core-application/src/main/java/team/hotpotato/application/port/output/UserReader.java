package team.hotpotato.application.port.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.user.User;

public interface UserReader {
    Mono<User> findByEmail(String email);
}
