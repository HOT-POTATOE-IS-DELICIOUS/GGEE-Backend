package team.hotpotato.domain.reaction.infrastructure.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * InMemoryIndexingJobCompletionEvents 의 Sink 전략별 Race Condition 재현 테스트.
 *
 * 재현 시나리오:
 *  - Kafka 가 all_done 이벤트를 SSE 클라이언트 연결 전에 먼저 전달하는 상황
 *  - 다른 사용자(User B)가 SSE 구독을 먼저 연결하는 상황
 *
 * 이 두 조건이 겹치면 User A 의 완료 이벤트가 유실되어 10분 timeout 까지 대기하게 된다.
 */
@DisplayName("인덱싱 완료 이벤트 Race Condition 테스트")
class IndexingJobCompletionEventsRaceConditionTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 수정 전: Sinks.many().multicast().onBackpressureBuffer()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[수정 전] 구독 전 emit 된 이벤트는 첫 번째 구독자가 버퍼를 소진하면 두 번째 구독자가 수신하지 못한다")
    void before_secondSubscriberMissesEventWhenFirstDrainsBuffer() {
        Sinks.Many<String> multicastSink = Sinks.many().multicast().onBackpressureBuffer(256);

        // t=0: Kafka 가 all_done 전달 → 구독자 없음 → 버퍼에 적재
        multicastSink.tryEmitNext("job-A");

        // t=50ms: User B 의 SSE 클라이언트가 먼저 연결 → 버퍼의 "job-A" 소진
        //         filter 불일치이므로 User B 는 계속 대기
        multicastSink.asFlux()
                .filter("job-B"::equals)
                .next()
                .subscribe();

        // t=100ms: User A 의 SSE 클라이언트 연결 → 버퍼는 이미 비워진 상태
        //          "job-A" 를 받지 못하고 timeout 까지 대기 → 버그 재현
        StepVerifier.withVirtualTime(() ->
                        multicastSink.asFlux()
                                .filter("job-A"::equals)
                                .next()
                                .timeout(Duration.ofMinutes(10))
                )
                .thenAwait(Duration.ofMinutes(10))
                .expectError(TimeoutException.class)
                .verify();
    }

    @Test
    @DisplayName("[수정 전] 구독자가 전혀 없을 때만 emit 하면 첫 번째 구독자는 버퍼를 수신한다 (정상 케이스)")
    void before_firstSubscriberReceivesBufferedEvent() {
        Sinks.Many<String> multicastSink = Sinks.many().multicast().onBackpressureBuffer(256);

        // 구독자 없을 때 emit → 버퍼에 적재
        multicastSink.tryEmitNext("job-A");

        // 첫 번째(이자 유일한) 구독자 → 버퍼의 이벤트 수신
        StepVerifier.create(
                        multicastSink.asFlux()
                                .filter("job-A"::equals)
                                .next()
                )
                .expectNext("job-A")
                .verifyComplete();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 수정 후: Sinks.many().replay().limit(Duration.ofMinutes(2))
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[수정 후] 모든 늦은 구독자가 시간 창 내 이벤트를 수신한다")
    void after_allLateSubscribersReceiveReplayedEvent() {
        Sinks.Many<String> replaySink = Sinks.many().replay().limit(Duration.ofMinutes(2));

        // t=0: Kafka all_done 도착 → 구독자 없어도 replay 캐시에 저장
        replaySink.tryEmitNext("job-A");

        // t=50ms: User B 의 SSE 클라이언트 먼저 연결 → replay 에서 "job-A" 수신하지만 filter 불일치
        replaySink.asFlux()
                .filter("job-B"::equals)
                .next()
                .subscribe();

        // t=100ms: User A 의 SSE 클라이언트 연결 → replay 덕분에 "job-A" 수신 → 즉시 완료
        StepVerifier.create(
                        replaySink.asFlux()
                                .filter("job-A"::equals)
                                .next()
                )
                .expectNext("job-A")
                .verifyComplete();
    }

    @Test
    @DisplayName("[수정 후] 여러 사용자가 동시에 각자의 jobId 이벤트를 정확히 수신한다")
    void after_multipleSubscribersEachReceiveTheirOwnEvent() {
        Sinks.Many<String> replaySink = Sinks.many().replay().limit(Duration.ofMinutes(2));

        // 두 이벤트를 구독 전에 미리 emit
        replaySink.tryEmitNext("job-A");
        replaySink.tryEmitNext("job-B");

        // User A: job-A 수신
        StepVerifier.create(
                        replaySink.asFlux()
                                .filter("job-A"::equals)
                                .next()
                )
                .expectNext("job-A")
                .verifyComplete();

        // User B: job-B 수신
        StepVerifier.create(
                        replaySink.asFlux()
                                .filter("job-B"::equals)
                                .next()
                )
                .expectNext("job-B")
                .verifyComplete();
    }

    @Test
    @DisplayName("[수정 후] replay 시간 창 밖의 이벤트는 재전송되지 않는다")
    void after_eventOutsideReplayWindowIsNotReplayed() throws InterruptedException {
        // Sinks.replay().limit(Duration) 의 TTL 은 실제 시간 기반 → Thread.sleep 으로 측정
        Sinks.Many<String> replaySink = Sinks.many().replay().limit(Duration.ofMillis(100));

        replaySink.tryEmitNext("old-job");

        // replay 창(100ms) 이 지나도록 대기
        Thread.sleep(200);

        // 창이 지난 이벤트 → 새 구독자에게 재전송되지 않아야 함
        StepVerifier.create(
                        replaySink.asFlux()
                                .filter("old-job"::equals)
                                .next()
                                .timeout(Duration.ofMillis(300))
                )
                .expectError(TimeoutException.class)
                .verify();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 실제 컴포넌트 테스트 (수정 후)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[컴포넌트] InMemoryIndexingJobCompletionEvents: 늦은 구독자도 완료 이벤트를 수신한다")
    void component_lateSubscriberReceivesCompletionEvent() {
        InMemoryIndexingJobCompletionEvents events = new InMemoryIndexingJobCompletionEvents();

        // Kafka Streams 가 all_done 처리 → SSE 클라이언트 연결 전
        events.complete("job-late-subscriber");

        // SSE 클라이언트가 늦게 연결해도 replay 덕분에 즉시 수신
        StepVerifier.create(
                        events.completions()
                                .filter("job-late-subscriber"::equals)
                                .next()
                )
                .expectNext("job-late-subscriber")
                .verifyComplete();
    }

    @Test
    @DisplayName("[컴포넌트] InMemoryIndexingJobCompletionEvents: emit 실패 없이 256개 이상 처리 가능하다")
    void component_emitSucceedsForManyEvents() {
        InMemoryIndexingJobCompletionEvents events = new InMemoryIndexingJobCompletionEvents();

        // replay 는 buffer 크기 제한이 없으므로 대량 emit 도 실패하지 않음
        for (int i = 0; i < 300; i++) {
            Sinks.EmitResult result = Sinks.EmitResult.OK;
            events.complete("job-" + i);
            // 실패 없이 emit 완료 검증은 로그 없이 수행 (warn 없으면 OK)
        }

        StepVerifier.create(
                        events.completions()
                                .filter("job-299"::equals)
                                .next()
                )
                .expectNext("job-299")
                .verifyComplete();
    }
}
