package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Flux;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;

public interface ProtectTargetIndexingOutboxReader {
    Flux<ProtectTargetIndexingOutbox> findPending();
}
