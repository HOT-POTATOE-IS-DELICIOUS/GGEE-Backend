package team.hotpotato.domain.protect.application.output;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.domain.Protect;
import team.hotpotato.domain.protect.domain.ProtectTargetSnapshot;

public interface ProtectRepository {
    Mono<Protect> save(Protect protect);

    Mono<Protect> findByUserId(Long userId);

    Flux<ProtectTargetSnapshot> findActiveDistinctProtectTargets();
}
