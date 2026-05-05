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
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxScheduler {

    private static final Duration STALE_CLAIM_THRESHOLD = Duration.ofMinutes(5);

    private final ProtectTargetIndexingOutboxDispatchUseCase dispatchUseCase;
    private final ProtectTargetIndexingOutboxRepository outboxRepository;

    @Value("${ggee.member.protect-target-indexing-dispatch-delay}")
    private long delayMillis;

    private Disposable subscription;

    // @PostConstruct는 Spring 라이프사이클 진입점이므로 subscribe() 호출이 허용된다.
    @PostConstruct
    public void start() {
        Mono<Void> recoverStaleClaim = outboxRepository.recoverStaleClaimed(STALE_CLAIM_THRESHOLD)
                .doOnNext(recovered -> {
                    if (recovered > 0) {
                        log.warn("부팅 시 stuck IN_PROGRESS outbox {}건을 PENDING으로 회수했습니다.", recovered);
                    }
                })
                .doOnError(e -> log.error("부팅 시 stuck outbox 회수 실패", e))
                .onErrorResume(e -> Mono.empty())
                .then();

        subscription = recoverStaleClaim
                .thenMany(Flux.interval(Duration.ofMillis(delayMillis)))
                .onBackpressureDrop()
                .concatMap(t -> dispatchUseCase.dispatchPending()
                        .doOnError(e -> log.error("보호 대상 인덱싱 outbox 스케줄러 실패", e))
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
