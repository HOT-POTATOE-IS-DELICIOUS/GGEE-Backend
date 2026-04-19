package team.hotpotato.domain.member.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import team.hotpotato.domain.member.application.output.PasswordHasher;

@Component
@RequiredArgsConstructor
public class PasswordHasherAdapter implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<String> hash(String rawPassword) {
        return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> matches(String rawPassword, String hashedPassword) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, hashedPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
