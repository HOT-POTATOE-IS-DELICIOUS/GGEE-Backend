package team.hotpotato.domain.protect.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.dto.ProtectTargetIndexingPublishCommand;

public interface ProtectTargetIndexingPublisher {
    Mono<Void> publish(ProtectTargetIndexingPublishCommand command);
}
