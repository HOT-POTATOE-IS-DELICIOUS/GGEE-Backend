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
import team.hotpotato.domain.protect.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxScheduler {

    private final ProtectTargetIndexingOutboxDispatchUseCase dispatchUseCase;

    @Value("${ggee.member.protect-target-indexing-dispatch-delay}")
    private long delayMillis;

    private Disposable subscription;

    // @PostConstruct는 Spring 라이프사이클 진입점이므로 subscribe() 호출이 허용된다.
    @PostConstruct
    public void start() {
        subscription = Flux.interval(Duration.ofMillis(delayMillis))
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
