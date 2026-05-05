package team.hotpotato.domain.protect.infrastructure.indexing;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.input.ProtectTargetRefreshSchedule;

import java.time.Duration;

/**
 * 단일 replica 운영 가정. 다중 replica로 확장 시 동일 tick이 모든 파드에서 발화하여
 * outbox 행이 중복 적재되므로 분산 락(ShedLock 또는 Postgres advisory lock)을 함께 도입해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetRefreshScheduler {

    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(30);
    private static final Duration INITIAL_DELAY = Duration.ofMinutes(1);

    private final ProtectTargetRefreshSchedule refreshUseCase;

    private Disposable subscription;

    // @PostConstruct는 Spring 라이프사이클 진입점이므로 subscribe() 호출이 허용된다.
    @PostConstruct
    public void start() {
        subscription = Flux.interval(INITIAL_DELAY, REFRESH_INTERVAL)
                .onBackpressureDrop()
                .concatMap(t -> refreshUseCase.scheduleAll()
                        .doOnError(e -> log.error("보호 대상 갱신 스케줄러 실패", e))
                        .onErrorResume(e -> Mono.empty()))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
