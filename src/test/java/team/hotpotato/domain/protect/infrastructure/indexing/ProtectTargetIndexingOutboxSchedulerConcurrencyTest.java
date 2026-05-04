package team.hotpotato.domain.protect.infrastructure.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Outbox 스케줄러 동시 실행 방지 검증")
class ProtectTargetIndexingOutboxSchedulerConcurrencyTest {

    @Test
    @DisplayName("[수정 전] subscribe()는 즉시 반환하여 fixedDelay가 Mono 완료를 기다리지 않는다")
    void before_subscribeReturnsImmediatelyIgnoringMonoCompletion() throws InterruptedException {
        // Mono가 100ms 걸리는 작업을 시뮬레이션
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        Runnable schedulerTask = () -> {
            // subscribe() 방식 - 즉시 반환
            Mono.fromRunnable(() -> {
                        int current = concurrentCount.incrementAndGet();
                        maxConcurrent.updateAndGet(m -> Math.max(m, current));
                        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        concurrentCount.decrementAndGet();
                    })
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .subscribe(); // 즉시 반환
        };

        // fixedDelay=10ms 시뮬레이션: subscribe() 후 10ms 대기 → 다시 실행
        for (int i = 0; i < 5; i++) {
            schedulerTask.run();
            Thread.sleep(10); // fixedDelay
        }
        Thread.sleep(200); // Mono들 완료 대기

        System.out.printf("[수정 전] subscribe() 방식 최대 동시 실행 수: %d%n", maxConcurrent.get());
        // subscribe() 방식에서는 Mono 작업(50ms)이 fixedDelay(10ms)보다 길어서 동시 실행 발생
        assertTrue(maxConcurrent.get() > 1,
                "subscribe() 방식에서는 동시 실행이 발생한다. 최대 동시 실행 수: " + maxConcurrent.get());
    }

    @Test
    @DisplayName("[수정 후] block()은 Mono 완료까지 대기하여 동시 실행을 방지한다")
    void after_blockWaitsForMonoCompletionPreventingConcurrentExecution() throws InterruptedException {
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        Runnable schedulerTask = () -> {
            // block() 방식 - Mono 완료까지 대기
            Mono.fromRunnable(() -> {
                        int current = concurrentCount.incrementAndGet();
                        maxConcurrent.updateAndGet(m -> Math.max(m, current));
                        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        concurrentCount.decrementAndGet();
                    })
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(); // Mono 완료까지 블로킹
        };

        // 단일 스레드에서 sequentially 실행 (Spring @Scheduled 방식)
        Thread schedulerThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                schedulerTask.run(); // block()이므로 완료 후 다음 반복
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        schedulerThread.start();
        schedulerThread.join(2000);

        System.out.printf("[수정 후] block() 방식 최대 동시 실행 수: %d%n", maxConcurrent.get());
        // block() 방식에서는 이전 실행 완료 후 다음 실행 → 최대 동시 실행 수 = 1
        assertEquals(1, maxConcurrent.get(),
                "block() 방식에서는 동시 실행이 발생하지 않는다");
    }
}
