package team.hotpotato.domain.protect.application.output;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;

import java.time.Duration;

public interface ProtectTargetIndexingOutboxRepository {
    Flux<ProtectTargetIndexingOutbox> findPending();

    Mono<ProtectTargetIndexingOutbox> save(ProtectTargetIndexingOutbox outbox);

    Mono<Long> claim(Long outboxId);

    Mono<Void> markPublished(Long outboxId);

    Mono<Void> markCompleted(Long outboxId);

    Mono<Void> rollbackToPending(Long outboxId);

    Mono<Long> recoverStaleClaimed(Duration staleThreshold);
}
