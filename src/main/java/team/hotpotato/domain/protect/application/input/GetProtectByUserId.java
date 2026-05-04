package team.hotpotato.domain.protect.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.domain.Protect;

public interface GetProtectByUserId {
    Mono<Protect> get(Long userId);
}
