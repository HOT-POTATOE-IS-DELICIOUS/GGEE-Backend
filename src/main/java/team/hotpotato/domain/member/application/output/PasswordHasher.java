package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;

public interface PasswordHasher {

    Mono<String> hash(String rawPassword);

    Mono<Boolean> matches(String rawPassword, String hashedPassword);
}
