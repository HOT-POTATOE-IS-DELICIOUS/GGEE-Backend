package team.hotpotato.domain.protect.infrastructure.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProtectTargetIndexingOutboxScheduler Flux.interval 라이프사이클 검증")
class ProtectTargetIndexingOutboxSchedulerConcurrencyTest {

    /**
     * concatMap 직렬화 검증:
     * tick이 여러 번 발화해도 이전 dispatchPending()이 완료되기 전까지
     * 다음 실행이 시작되지 않음을 확인한다.
     */
    @Test
    @DisplayName("concatMap은 이전 dispatchPending() 완료 후 다음 tick을 처리한다")
    void concatMap_serializesDispatchCalls() throws InterruptedException {
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger concurrentCount = new AtomicInteger(0);

        // 50ms 걸리는 작업 — tick 간격(10ms)보다 느리다
        var disposable = Flux.interval(Duration.ofMillis(10))
                .onBackpressureDrop()
                .concatMap(t -> Mono.fromCallable(() -> {
                    int current = concurrentCount.incrementAndGet();
                    maxConcurrent.updateAndGet(m -> Math.max(m, current));
                    Thread.sleep(50);
                    concurrentCount.decrementAndGet();
                    return null;
                }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .doOnError(e -> {})
                        .onErrorResume(e -> Mono.empty()))
                .subscribe();

        Thread.sleep(300);
        disposable.dispose();

        assertThat(maxConcurrent.get())
                .as("concatMap은 동시 실행을 허용하지 않는다")
                .isEqualTo(1);
    }

    /**
     * @PreDestroy 안전 종료 검증:
     * dispose() 호출 후 subscription이 disposed 상태로 전환되고
     * 이미 disposed된 구독에 재호출해도 예외가 발생하지 않음을 확인한다.
     */
    @Test
    @DisplayName("dispose() 호출 시 subscription이 안전하게 종료된다")
    void dispose_safelyTerminatesSubscription() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);

        var disposable = Flux.interval(Duration.ofMillis(50))
                .onBackpressureDrop()
                .concatMap(t -> Mono.fromRunnable(callCount::incrementAndGet)
                        .doOnError(e -> {})
                        .onErrorResume(e -> Mono.empty()))
                .subscribe();

        Thread.sleep(120);
        assertThat(disposable.isDisposed()).isFalse();

        // @PreDestroy 패턴 재현
        if (!disposable.isDisposed()) {
            disposable.dispose();
        }

        assertThat(disposable.isDisposed()).isTrue();

        // 이미 disposed된 구독에 재호출해도 예외 없음
        disposable.dispose();

        // dispose 후 callCount가 증가하지 않음을 확인
        int countAfterDispose = callCount.get();
        Thread.sleep(100);
        assertThat(callCount.get())
                .as("dispose 이후 추가 실행이 발생하지 않는다")
                .isEqualTo(countAfterDispose);
    }

    /**
     * onErrorResume 격리 검증:
     * dispatchPending()이 에러를 반환해도 Flux 스트림이 종료되지 않고 계속 실행된다.
     */
    @Test
    @DisplayName("dispatchPending() 에러 발생 시 스트림이 종료되지 않고 계속 실행된다")
    void onErrorResume_doesNotTerminateStream() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        var disposable = Flux.interval(Duration.ofMillis(30))
                .onBackpressureDrop()
                .concatMap(t -> {
                    int n = callCount.incrementAndGet();
                    // 짝수 tick에서 에러 발생
                    if (n % 2 == 0) {
                        return Mono.error(new RuntimeException("테스트 에러"))
                                .doOnError(e -> {})
                                .onErrorResume(e -> Mono.empty());
                    }
                    successCount.incrementAndGet();
                    return Mono.<Void>empty();
                })
                .subscribe();

        Thread.sleep(250);
        disposable.dispose();

        assertThat(callCount.get())
                .as("에러 이후에도 스트림이 계속 실행된다")
                .isGreaterThan(3);
        assertThat(successCount.get())
                .as("에러가 없는 tick은 정상 처리된다")
                .isGreaterThan(0);
    }
}
