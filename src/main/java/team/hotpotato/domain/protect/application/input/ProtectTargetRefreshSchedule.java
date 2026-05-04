package team.hotpotato.domain.protect.application.input;

import reactor.core.publisher.Mono;

public interface ProtectTargetRefreshSchedule {
    Mono<Long> scheduleAll();
}
