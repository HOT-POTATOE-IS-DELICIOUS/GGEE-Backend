package team.hotpotato.domain.member.application.persistence;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.User;

public interface UserReader {
    Mono<User> findByEmail(String email);
}
