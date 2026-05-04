package team.hotpotato.domain.protect.application.output;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;

public interface ProtectTargetIndexingOutboxRepository {
    Flux<ProtectTargetIndexingOutbox> findPending();

    Mono<ProtectTargetIndexingOutbox> save(ProtectTargetIndexingOutbox outbox);

    Mono<Void> markPublished(Long outboxId);

    Mono<Void> markCompleted(Long outboxId);
}
