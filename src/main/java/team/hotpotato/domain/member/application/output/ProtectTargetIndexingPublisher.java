package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.event.ProtectTargetIndexingMessage;

public interface ProtectTargetIndexingPublisher {
    Mono<Void> publish(ProtectTargetIndexingMessage message);
}
