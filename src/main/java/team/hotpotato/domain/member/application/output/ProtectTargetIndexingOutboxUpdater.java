package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;

public interface ProtectTargetIndexingOutboxUpdater {
    Mono<Void> markPublished(Long outboxId);
}
