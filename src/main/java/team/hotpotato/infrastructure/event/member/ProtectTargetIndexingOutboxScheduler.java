package team.hotpotato.infrastructure.event.member;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;

@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxScheduler {
    private final ProtectTargetIndexingOutboxDispatchUseCase dispatchUseCase;

    @Scheduled(fixedDelayString = "${ggee.member.protect-target-indexing-dispatch-delay}")
    public Mono<Void> dispatchPending() {
        return dispatchUseCase.dispatchPending();
    }
}
