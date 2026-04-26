# 인덱싱 완료 이벤트 Race Condition

## 문제

### 증상
사용자가 회원가입 후 인덱싱 완료 SSE 스트림(`GET /indexing/jobs/{job_id}`)을 열었을 때,
이미 완료된 작업임에도 응답이 오지 않고 **10분 타임아웃**까지 대기하는 현상이 발생했다.

### 재현 조건
1. Kafka 크롤링 완료 이벤트(`all_done`)가 SSE 클라이언트 연결보다 먼저 도달
2. 다른 사용자의 SSE 클라이언트가 먼저 구독을 시작

두 조건이 겹치면 완료 이벤트가 유실된다.

## 원인 분석

### 코드 흐름
```
UserRegisterUseCase
  → ProtectTargetIndexingOutbox 저장 (PENDING)
  → Outbox Relay → Kafka publish (PUBLISHED)

IndexingJobCompletionStreamConfiguration (Kafka Streams)
  → all_done 수신 → InMemoryIndexingJobCompletionEvents.complete(jobId)

IndexingJobController
  → GET /indexing/jobs/{job_id} → SSE 스트림
  → IndexingJobCompletionWaiterUseCase.waitForCompletion(jobId)
    → completionEvents.completions().filter(jobId::equals).next()
```

### 핵심 문제: Hot Multicast Publisher + 버퍼 소진

`Sinks.many().multicast().onBackpressureBuffer(256)` 의 동작:
- 구독자 없을 때 emit → **단일 공유 버퍼**에 적재
- **첫 번째 구독자만** 버퍼의 이벤트를 수신
- 이후 구독자는 새로 emit 된 이벤트만 수신

**Race Condition 시나리오:**

| 시점 | 이벤트 | 결과 |
|------|--------|------|
| t=0ms | Kafka `all_done` 도달, 구독자 없음 | 버퍼: `["job-A"]` |
| t=50ms | User B SSE 연결 (job-B 대기) | 버퍼 소진, `job-A` filter 불일치 → 계속 대기 |
| t=100ms | User A SSE 연결 (job-A 대기) | 버퍼 비어있음 → **이벤트 영구 유실** |
| t=10분 | User A timeout | `TimeoutException` |

## 수정

### 변경 파일
`InMemoryIndexingJobCompletionEvents.java`

### Before
```java
private static final int BUFFER_SIZE = 256;

private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(BUFFER_SIZE);
```

### After
```java
private static final Duration REPLAY_WINDOW = Duration.ofMinutes(2);

private final Sinks.Many<String> sink = Sinks.many().replay().limit(REPLAY_WINDOW);
```

### 수정 근거
`Sinks.many().replay().limit(Duration)` 는:
- emit 된 이벤트를 **시간 창(2분) 내에 구독하는 모든 subscriber에게 재전송**
- 첫 번째 구독자가 버퍼를 소진해도 이후 구독자에게 동일하게 전달
- 2분 창은 Kafka 처리 지연(통상 수백ms~수초) 대비 충분한 여유

## 검증

### 테스트 결과
```
> Task :test

BUILD SUCCESSFUL in 3s
5 actionable tasks: 2 executed, 3 up-to-date

Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

테스트 파일: `src/test/java/team/hotpotato/domain/reaction/infrastructure/indexing/IndexingJobCompletionEventsRaceConditionTest.java`

### 테스트 케이스 설명

| 테스트 | 목적 | 결과 |
|--------|------|------|
| `before_secondSubscriberMissesEventWhenFirstDrainsBuffer` | Race condition 버그 재현 (VirtualTime으로 10분 TimeoutException 검증) | PASS |
| `before_firstSubscriberReceivesBufferedEvent` | 단일 구독자 정상 케이스 | PASS |
| `after_allLateSubscribersReceiveReplayedEvent` | Race condition 수정 검증 | PASS |
| `after_multipleSubscribersEachReceiveTheirOwnEvent` | 다중 사용자 동시 수신 | PASS |
| `after_eventOutsideReplayWindowIsNotReplayed` | replay 창 경계 검증 | PASS |
| `component_lateSubscriberReceivesCompletionEvent` | 실제 컴포넌트 통합 검증 | PASS |
| `component_emitSucceedsForManyEvents` | 256개 초과 emit 안정성 | PASS |

### 정량적 개선

| 항목 | 수정 전 | 수정 후 |
|------|---------|---------|
| Race condition 발생 시 대기 시간 | **600,000ms** (10분 timeout) | **~0ms** (즉시 수신) |
| 이벤트 유실 가능 조건 | Kafka 처리 > SSE 연결 or 경쟁 구독자 존재 | **없음** (2분 창 내 보장) |
| 버퍼 한계 초과 emit 실패 | 256개 초과 시 `FAIL_OVERFLOW` | **없음** (replay 캐시는 크기 제한 없음) |

## 관련 컴포넌트

- `InMemoryIndexingJobCompletionEvents` — Sink 전략
- `IndexingJobCompletionWaiterUseCase` — 완료 대기 로직
- `IndexingJobCompletionStreamConfiguration` — Kafka Streams → Sink emit
- `IndexingJobController` — SSE 엔드포인트 (`GET /indexing/jobs/{job_id}`)
