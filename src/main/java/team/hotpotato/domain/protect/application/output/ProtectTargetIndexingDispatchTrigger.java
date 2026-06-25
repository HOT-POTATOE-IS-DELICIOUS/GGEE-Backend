package team.hotpotato.domain.protect.application.output;

import reactor.core.publisher.Mono;

public interface ProtectTargetIndexingDispatchTrigger {
    Mono<Void> requestDispatch();
}
