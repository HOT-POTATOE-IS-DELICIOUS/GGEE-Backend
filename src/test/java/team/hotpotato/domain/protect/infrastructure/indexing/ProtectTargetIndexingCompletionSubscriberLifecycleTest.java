package team.hotpotato.domain.protect.infrastructure.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CompletionSubscriber 라이프사이클 검증")
class ProtectTargetIndexingCompletionSubscriberLifecycleTest {

    @Test
    @DisplayName("[수정 전] Disposable을 저장하지 않으면 구독 해제 불가")
    void before_subscriptionCannotBeCancelledWithoutDisposable() {
        Sinks.Many<String> sink = Sinks.many().replay().limit(Duration.ofMinutes(2));
        Disposable[] captured = new Disposable[1];

        // subscribe() 반환값을 버리는 패턴
        sink.asFlux().subscribe(); // Disposable 버림

        // 외부에서 구독 해제할 방법이 없음
        assertNull(captured[0], "Disposable이 저장되지 않아 구독 해제 불가");
    }

    @Test
    @DisplayName("[수정 후] Disposable을 저장하면 @PreDestroy에서 구독 해제 가능")
    void after_subscriptionCanBeCancelledViaDisposable() {
        Sinks.Many<String> sink = Sinks.many().replay().limit(Duration.ofMinutes(2));

        // subscribe() 반환값을 필드에 저장하는 패턴
        Disposable subscription = sink.asFlux().subscribe();

        assertFalse(subscription.isDisposed(), "구독 활성 상태");

        // @PreDestroy에서 호출
        subscription.dispose();

        assertTrue(subscription.isDisposed(), "구독 해제 완료");
    }

    @Test
    @DisplayName("[수정 후] 구독 해제 후 새 이벤트가 처리되지 않는다")
    void after_noEventsProcessedAfterDispose() throws InterruptedException {
        Sinks.Many<String> sink = Sinks.many().replay().limit(Duration.ofMinutes(2));
        AtomicInteger processedCount = new AtomicInteger(0);

        Disposable subscription = sink.asFlux()
                .doOnNext(__ -> processedCount.incrementAndGet())
                .subscribe();

        sink.tryEmitNext("job-1");
        Thread.sleep(10);
        assertEquals(1, processedCount.get());

        // 구독 해제 (앱 종료 시뮬레이션)
        subscription.dispose();

        sink.tryEmitNext("job-2"); // 해제 후 이벤트
        Thread.sleep(10);
        assertEquals(1, processedCount.get(), "구독 해제 후 이벤트는 처리되지 않는다");
    }
}
