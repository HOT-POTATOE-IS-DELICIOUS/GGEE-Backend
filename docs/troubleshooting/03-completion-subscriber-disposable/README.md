# CompletionSubscriber Disposable 누락: 앱 종료 시 구독 미해제

## 문제

`@PostConstruct`에서 `subscribe()`의 반환값(`Disposable`)을 저장하지 않아 애플리케이션 종료 시 구독을 명시적으로 해제할 수 없다. 결과적으로:
- Spring Context 종료 후에도 Sink로부터의 이벤트 처리 시도가 지속됨
- 구독이 유지된 채 GC 대상이 되지 않아 리소스 누수 발생

### Before
```java
@PostConstruct
public void subscribe() {
    completionEvents.completions()
            .flatMap(...)
            .subscribe(); // Disposable 반환값 버림
}
// @PreDestroy 없음 → 앱 종료 시 구독 해제 불가
```

## 해결

`Disposable subscription` 필드를 추가하고 `@PreDestroy`에서 `dispose()` 호출.

### After
```java
private Disposable subscription;

@PostConstruct
public void subscribe() {
    subscription = completionEvents.completions()
            .flatMap(...)
            .subscribe();
}

@PreDestroy
public void cleanup() {
    if (subscription != null && !subscription.isDisposed()) {
        subscription.dispose();
    }
}
```

### 변경 파일
`ProtectTargetIndexingCompletionSubscriber.java`

## 검증

테스트 파일: `src/test/java/team/hotpotato/domain/member/infrastructure/indexing/ProtectTargetIndexingCompletionSubscriberLifecycleTest.java`

| 테스트 | 결과 |
|--------|------|
| `before_subscriptionCannotBeCancelledWithoutDisposable` | PASS |
| `after_subscriptionCanBeCancelledViaDisposable` | PASS |
| `after_noEventsProcessedAfterDispose` | PASS |

| 지표 | 수정 전 | 수정 후 |
|------|---------|---------|
| 종료 시 구독 해제 가능 여부 | 불가 | **가능** |
| 종료 후 이벤트 처리 여부 | 계속 처리 | **즉시 중단** |
