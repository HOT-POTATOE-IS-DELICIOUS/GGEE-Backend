package team.hotpotato.domain.protect.infrastructure.indexing;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingDispatchTrigger;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxScheduler implements ProtectTargetIndexingDispatchTrigger {

    private final ProtectTargetIndexingOutboxDispatchUseCase dispatchUseCase;
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final Sinks.Many<Object> dispatchRequests = Sinks.many().unicast().onBackpressureBuffer();
    private static final Duration DISPATCH_POLL_INTERVAL = Duration.ofSeconds(1);

    /**
     * 부팅 시 IN_PROGRESS 상태로 stuck된 outbox 행을 PENDING으로 회수하기 위한 임계값.
     * 정상 dispatch(claim → publish → markPublished)가 이 시간 이상 걸릴 일은 거의 없어야 한다.
     * Kafka send timeout과 정합되도록 운영 환경별 튜닝 가능.
     */
    @Value("${ggee.member.protect-target-indexing-stale-claim-threshold:5m}")
    private Duration staleClaimThreshold;

    @Value("${ggee.member.protect-target-indexing-dispatch-enabled:true}")
    private boolean dispatchEnabled;

    private Disposable subscription;
    private static final Object DISPATCH_SIGNAL = new Object();

    @Override
    public Mono<Void> requestDispatch() {
        if (!dispatchEnabled) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> {
            try {
                dispatchRequests.emitNext(
                        DISPATCH_SIGNAL,
                        Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100))
                );
            } catch (Sinks.EmissionException e) {
                log.warn("보호 대상 인덱싱 outbox dispatch trigger emit 실패", e);
            }
        });
    }

    // @PostConstruct는 Spring 라이프사이클 진입점이므로 subscribe() 호출이 허용된다.
    @PostConstruct
    public void start() {
        if (!dispatchEnabled) {
            log.info("보호 대상 인덱싱 outbox dispatcher가 비활성화되었습니다.");
            return;
        }

        Mono<Void> recoverStaleClaim = outboxRepository.recoverStaleClaimed(staleClaimThreshold)
                .doOnNext(recovered -> {
                    if (recovered > 0) {
                        log.warn("부팅 시 stuck IN_PROGRESS outbox {}건을 PENDING으로 회수했습니다.", recovered);
                    }
                })
                .doOnError(e -> log.error("부팅 시 stuck outbox 회수 실패", e))
                .onErrorResume(e -> Mono.empty())
                .then();

        subscription = recoverStaleClaim
                .then(dispatchOnce())
                .thenMany(Flux.merge(
                        dispatchRequests.asFlux(),
                        Flux.interval(DISPATCH_POLL_INTERVAL).map(tick -> DISPATCH_SIGNAL)
                ))
                .concatMap(signal -> dispatchOnce())
                .subscribe();
    }

    private Mono<Void> dispatchOnce() {
        return dispatchUseCase.dispatchPending()
                .doOnError(e -> log.warn("보호 대상 인덱싱 outbox 스케줄러 실패. cause={}", e.toString()))
                .onErrorResume(e -> Mono.empty());
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
