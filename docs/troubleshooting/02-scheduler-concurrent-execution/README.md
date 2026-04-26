# Scheduler Fire-and-Forget: 동시 실행으로 인한 중복 Outbox 처리

## 문제

`@Scheduled(fixedDelay)`와 `subscribe()`를 함께 사용하면 `subscribe()`가 즉시 반환하기 때문에 Spring 스케줄러는 Mono 완료를 기다리지 않고 다음 실행을 즉시 예약한다. Mono 처리 시간이 `fixedDelay`보다 길면 이전 실행이 끝나기 전에 새 실행이 시작되어 동시 실행이 발생한다.

```
// 수정 전 동작 (fixedDelay=2s, Mono 처리 3s 가정)
t=0s  : 실행 1 시작 → subscribe() 즉시 반환 → fixedDelay 카운트 시작
t=2s  : 실행 2 시작 (실행 1은 t=3s까지 실행 중)
        → 실행 1, 2 동시에 같은 PENDING outbox 조회·발행 (중복)
t=3s  : 실행 1 완료
t=4s  : 실행 2 완료
```

### Before
```java
@Scheduled(fixedDelayString = "${ggee.member.protect-target-indexing-dispatch-delay}")
public void dispatchPending() {
    dispatchUseCase.dispatchPending()
            .doOnError(e -> log.error("보호 대상 인덱싱 outbox 스케줄러 실패", e))
            .onErrorResume(e -> Mono.empty())
            .subscribe(); // 즉시 반환 → fixedDelay 카운트 시작
}
```

## 해결

`subscribe()` → `block()` 으로 변경. `fixedDelay`는 `block()` 반환(= Mono 완료) 이후부터 카운트되어 동시 실행이 구조적으로 불가능해진다.

### After
```java
@Scheduled(fixedDelayString = "${ggee.member.protect-target-indexing-dispatch-delay}")
public void dispatchPending() {
    dispatchUseCase.dispatchPending()
            .doOnError(e -> log.error("보호 대상 인덱싱 outbox 스케줄러 실패", e))
            .onErrorResume(e -> Mono.empty())
            .block(); // Mono 완료 후 fixedDelay 카운트 시작
}
```

### 변경 파일
`ProtectTargetIndexingOutboxScheduler.java`

## 검증

테스트 파일: `src/test/java/team/hotpotato/domain/member/infrastructure/indexing/ProtectTargetIndexingOutboxSchedulerConcurrencyTest.java`

측정 조건: Mono 작업 50ms, fixedDelay 10ms, 5회 반복

```
[수정 전] subscribe() 방식 최대 동시 실행 수: 5
[수정 후] block()    방식 최대 동시 실행 수: 1
```

| 지표 | 수정 전 | 수정 후 |
|------|---------|---------|
| 최대 동시 실행 수 (50ms 작업 + 10ms delay, 5회) | **5** | **1** |
| 중복 Outbox 발행 가능성 | 있음 | **없음** |
| fixedDelay 카운트 시작 시점 | subscribe() 반환 즉시 | **Mono 완료 후** |
