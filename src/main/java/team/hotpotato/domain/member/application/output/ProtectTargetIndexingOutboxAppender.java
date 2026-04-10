package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;

public interface ProtectTargetIndexingOutboxAppender {
    Mono<ProtectTargetIndexingOutbox> save(ProtectTargetIndexingOutbox outbox);
}
