package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.dto.ProtectTargetIndexingPublishCommand;

public interface ProtectTargetIndexingPublisher {
    Mono<Void> publish(ProtectTargetIndexingPublishCommand command);
}
