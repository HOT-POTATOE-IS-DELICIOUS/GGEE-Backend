package team.hotpotato.infrastructure.event.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.hotpotato.domain.member.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxScheduler {
    private final ProtectTargetIndexingOutboxDispatchUseCase dispatchUseCase;

    @Scheduled(fixedDelayString = "${ggee.member.protect-target-indexing-dispatch-delay}")
    public void dispatchPending() {
        dispatchUseCase.dispatchPending()
                .doOnError(e -> log.error("보호 대상 인덱싱 outbox 스케줄러 실패", e))
                .subscribe();
    }
}
