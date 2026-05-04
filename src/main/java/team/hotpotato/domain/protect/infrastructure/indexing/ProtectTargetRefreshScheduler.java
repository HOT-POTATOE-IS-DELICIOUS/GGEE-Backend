package team.hotpotato.domain.protect.infrastructure.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.input.ProtectTargetRefreshSchedule;

/**
 * 단일 replica 운영 가정. 다중 replica로 확장 시 동일 cron tick이 모든 파드에서 발화하여
 * outbox 행이 중복 적재되므로 분산 락(ShedLock 또는 Postgres advisory lock)을 함께 도입해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetRefreshScheduler {
    private final ProtectTargetRefreshSchedule refreshUseCase;

    @Scheduled(cron = "${ggee.member.protect-target-refresh-cron}")
    public void refresh() {
        refreshUseCase.scheduleAll()
                .doOnError(e -> log.error("보호 대상 갱신 스케줄러 실패", e))
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
