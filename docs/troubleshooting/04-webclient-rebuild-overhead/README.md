# WebClient 매 요청 재생성: Builder 스레드 비안전 + 인스턴스 생성 오버헤드

## 문제

`NodeNewsHttpSource`, `AuditHttpSource`, `IssueGraphHttpSource`, `StrategyAiClientAdapter` 4개 HTTP 클라이언트에서 `webClientBuilder.baseUrl(...).build()`를 매 요청마다 호출하고 있다.

`WebClient.Builder`는 **스레드 비안전(not thread-safe)**하며, `baseUrl()`은 빌더 내부 상태(`uriBuilderFactory`)를 변경하고 `this`를 반환한다. 멀티스레드 환경에서 여러 요청이 동시에 공유 Builder의 `baseUrl()`을 호출하면 내부 상태 경합이 발생할 수 있다. 또한 `build()` 자체가 코덱 체인·필터 체인을 매번 초기화하는 비용이 있다.

### Before
```java
// 4개 파일 모두 동일한 패턴 (IssueGraphHttpSource 기준)
@Override
public Mono<IssueGraph> read(String protectTarget, String protectTargetInfo) {
    return webClientBuilder.baseUrl(properties.baseUrl()).build()  // 스레드 비안전, 매번 초기화
            .get()
            .uri(uriBuilder -> { ... })
            ...
}
```

## 해결

`@PostConstruct`에서 한 번만 빌드하여 필드에 저장. `WebClient` 인스턴스 자체는 불변(immutable)이므로 스레드 간 공유 안전.

### After
```java
// 4개 파일 공통 패턴
private WebClient webClient;

@PostConstruct
public void init() {
    this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
}

@Override
public Mono<IssueGraph> read(String protectTarget, String protectTargetInfo) {
    return webClient.get()  // 불변 인스턴스 재사용
            .uri(uriBuilder -> { ... })
            ...
}
```

### 수정 대상 파일
- `NodeNewsHttpSource.java`
- `AuditHttpSource.java`
- `IssueGraphHttpSource.java`
- `StrategyAiClientAdapter.java`

## 검증

### ① JVM 마이크로벤치마크

테스트 파일: `src/test/java/team/hotpotato/domain/reaction/infrastructure/client/WebClientBuildOverheadTest.java`

1,000회 반복 측정 결과:

```
[수정 전] WebClient.build() 1000회: 총 8ms, 회당 2.17μs
[수정 후] WebClient 재사용  1000회: 총 0ms, 회당 0.052μs

=== WebClient 오버헤드 비교 결과 ===
build()  1회: 2.17μs
reuse()  1회: 0.052μs
build/reuse 배율: 42.1x
1,000 RPS 기준 초당 낭비: 2.17ms
```

| 항목 | 수정 전 | 수정 후 |
|------|---------|---------|
| `build()` 호출 오버헤드 | **2.17μs/req** | **0.052μs/req** (42배 감소) |
| 스레드 안전성 | **비안전** (shared Builder 상태 경합) | **안전** (불변 인스턴스 공유) |
| 1,000 RPS 기준 누적 낭비 | **2.17ms/s** | **0.052ms/s** |

### ② k6 부하 테스트 (`GET /issues` E2E)

k6 스크립트: [`k6-issues-bench.js`](./k6-issues-bench.js)

테스트 조건:
- 환경: Docker (Redpanda + PostgreSQL 17) + Spring Boot 로컬 실행
- AI 서버: 실제 서버(`192.168.75.240:8001`) 포함 E2E
- 부하: `5s→20 VUs → 20s→100 VUs → 5s→0` (max 100 VUs, 30s)

| 지표 | 수정 전 | 수정 후 |
|------|---------|---------|
| 처리량 (RPS) | 432.2 | 433.8 |
| 평균 응답시간 | 115.6ms | 115.1ms |
| p(95) | 327ms | 347ms |
| 에러율 | 0.00% | 0.00% |
| 총 요청 수 | 12,967 | 13,019 |

> **E2E 응답시간은 AI 서버 네트워크 지연(avg ~115ms)이 지배적**이어서 수정 전후 차이가 노이즈 수준이다.
> `build()` 42배 오버헤드는 마이크로벤치마크에서 확인되나, 실제 HTTP 엔드포인트에서는 네트워크 지연에 묻힌다.
>
> **핵심 개선은 정확성(Correctness)**: 동시에 여러 스레드가 공유 `WebClient.Builder` 인스턴스의 상태를 변경하는 비결정적 동작을 제거했다.
