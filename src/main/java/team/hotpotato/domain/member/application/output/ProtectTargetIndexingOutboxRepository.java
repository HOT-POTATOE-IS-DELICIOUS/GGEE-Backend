package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;

public interface ProtectTargetIndexingOutboxRepository {
    Flux<ProtectTargetIndexingOutbox> findPending();

    Mono<ProtectTargetIndexingOutbox> save(ProtectTargetIndexingOutbox outbox);

    Mono<Void> markPublished(Long outboxId);
}
