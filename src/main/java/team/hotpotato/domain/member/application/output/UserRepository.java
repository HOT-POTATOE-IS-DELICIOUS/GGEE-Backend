package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.User;

public interface UserRepository {
    Mono<User> findById(Long userId);

    Mono<User> findByEmail(String email);

    Mono<User> save(User user);
}
