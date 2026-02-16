# AGENTS.md

## Repository overview (30s)

- Project:
    - Spring Boot + Spring WebFlux 기반 Java 백엔드 프로젝트
- One-liner:
    - 공개 전 콘텐츠를 다중 사회·이념 관점에서 자동 검수해 평판 리스크를 조기 탐지·완화하고, 공개 후 여론 전개를 모니터링·역추적하는 브랜드 평판 방화벽 플랫폼 백엔드.
- Goal for agents:
    - CLI에서 빠르게 구조를 파악하고, 변경 범위를 최소화하며, 팀 규칙에 맞는 고품질 수정/기능 추가를 수행한다.
- Tech:
    - Java (LTS 버전 사용)
    - Spring Boot / WebFlux (Reactive)
    - Gradle
    - R2DBC MySQL
    - Kafka

## What to read first (navigation)

- Start here:
    - `settings.gradle` / 루트 `build.gradle`
    - `:core-api`(Web entry adapter)와 `:core-bootstrap`(실행 엔트리) 확인
    - Security 설정 (인증/인가)
    - Error handling 설정 (global error)
    - Persistence 어댑터 (R2DBC/Mongo/Redis 등)
- Search keywords:
    - `RouterFunction`, `HandlerFunction`, `SecurityWebFilterChain`, `ErrorWebExceptionHandler`
    - `TransactionalOperator`, `DatabaseClient`, `R2dbcEntityTemplate`, `ReactiveCrudRepository`

## Module / package map

- Modules:
    - `:core-bootstrap`      -> Spring Boot 실행 엔트리/모듈 조립
    - `:core-api`            -> WebFlux routers/handlers, request/response adapters
    - `:core-application`    -> use-cases, orchestration (reactive)
    - `:core-domain`         -> pure domain model (no Spring deps)
    - `:core-infrastructure` -> DB/JWT/설정 등 adapter 구현
- Dependency direction (rule):
    - `core-bootstrap → core-api → core-application → core-domain`
    - `core-bootstrap → core-infrastructure → (core-application, core-domain)`
    - `core-infrastructure` implements ports; `core-domain` must not depend on Spring or infrastructure.
- Package prefix convention (STRICT):
    - `core-api`            -> `team.hotpotato.api...`
    - `core-application`    -> `team.hotpotato.application...`
    - `core-domain`         -> `team.hotpotato.domain...`
    - `core-infrastructure` -> `team.hotpotato.infrastructure...`
    - `core-common`         -> `team.hotpotato.common...`
    - `core-bootstrap`      -> `team.hotpotato.bootstrap...`
    - 신규 코드에서 `team.hotpotato.ggeebackend...` 패키지는 사용하지 않는다.
    - 파일 경로와 `package` 선언은 항상 일치해야 한다.

## How to run (CLI)

- Dependencies:
    - `docker compose up -d` (if available)
- Run app:
    - `./gradlew :core-bootstrap:bootRun`
- Run tests:
    - `./gradlew test`
- Run full checks:
    - `./gradlew check`
- Formatting (if configured):
    - `./gradlew spotlessApply` or `./gradlew fmt`

## Coding rules (highest priority)

### Naming / DTO rules (STRICT)

- Layer별 DTO 네이밍 규칙:
    - API 계층: `request / response`
      (예: `...api.dto.request.*`, `...api.dto.response.*`)
    - Application 계층: `command / result / model`
      (예: `...application.dto.command.*`, `...application.dto.result.*`, `...application.dto.model.*`)
- Application 계층에 `request/response` 네이밍 DTO를 새로 만들지 않는다.
- API DTO를 Application으로 직접 전달하지 말고, Controller에서 `request -> command`, `result -> response`로 매핑한다.
- 클래스명은 책임 중심으로 짓고 기술 종속 네이밍을 피한다.
  (예: domain/application에서 `R2dbc`, `Jwt`, `MySql` 같은 기술명을 타입명에 노출하지 않는다)

### Reactive (WebFlux) rules

- Do NOT call `block()` or `subscribe()` in production code.
    - Allowed only in: tests / one-off local CLI utilities (explicitly documented).
- Avoid blocking I/O on event-loop threads.
    - If unavoidable, use `boundedElastic` or dedicated scheduler and document the reason.
- Use operators correctly:
    - `map`: pure transform
    - `flatMap`: async boundary
    - `then`: sequencing when previous value is not needed
    - Side effects: prefer `doOnNext/doOnSuccess/doOnError` for logging only (no state mutation).

### Transaction rules (STRICT)

- Transactions are NOT default. 무분별한 트랜잭션 사용 금지.
    - 단일 insert/update/delete 1회, 단순 CRUD, read-only 흐름에는 트랜잭션을 사용하지 않는다.
    - 트랜잭션은 리소스/락/컨텍스트 비용이 있으므로 “필요한 곳에만 최소 범위로” 적용한다.

- Use a transaction ONLY when atomicity is REQUIRED:
    - “A가 실패하면 B도 반드시 롤백”되어야 하는 원자성(all-or-nothing)이 요구될 때만 트랜잭션을 사용한다.
    - Examples (transaction required):
        - 2개 이상 DB write가 하나의 업무 단위로 묶여야 함
          (예: 주문 생성 + 재고 차감 + 결제 상태 기록)
        - 비즈니스 불변식(invariant)을 지키기 위한 다중 쓰기
          (예: 포인트 차감 + 이력 적재)

- Examples (transaction NOT required):
    - 단일 저장/업데이트 1회
    - 조회-only
    - 캐시 갱신, 로그 적재, 알림 발송, 메시지 발행 같은 부수효과
      → 실패해도 본 DB 작업을 롤백시키면 안 되는 작업은 트랜잭션 밖으로 분리
      (이벤트/비동기/아웃박스 패턴 등 고려)

- Reactive transaction style:
    - R2DBC/WebFlux에서는 `TransactionalOperator`(또는 프로젝트 표준)에 따라 구현한다.
    - DB 트랜잭션 안에 원격 호출(HTTP), 메시지 publish(Kafka), 파일 I/O를 포함하지 않는다.
    - 트랜잭션 경계는 “원자성이 필요한 DB write”까지만 최소화한다.

- Rollback policy:
    - 트랜잭션 파이프라인에서 에러가 발생하면 error 신호가 전파되어 롤백되도록 작성한다.
    - 트랜잭션 내부에서 `onErrorResume`로 에러를 삼키지 않는다(명시적 보상 트랜잭션 제외).

- Checklist before adding a transaction:
    1) 2개 이상 DB write가 있으며 all-or-nothing이 반드시 필요하다.
    2) 부분 성공이 비즈니스 불변식을 깨거나 잘못된 상태를 외부에 노출한다.
    3) 트랜잭션 경계에 원격 호출/메시징/블로킹 I/O가 포함되지 않는다.

### Error handling & validation

- Global error handling entry:
    - Prefer a single global handler (e.g., `ErrorWebExceptionHandler` or `@RestControllerAdvice`) and keep mapping
      consistent.
- Domain exceptions:
    - 도메인 예외는 의미 있는 타입으로 분리하고, HTTP 응답 매핑은 한 곳에서 처리한다.
- Validation:
    - 입력 검증 규칙을 일관되게 적용한다(Bean Validation 사용 여부는 프로젝트 설정에 따름).

### Logging / Observability

- Log with correlation id if configured.
- Do not log secrets (tokens/passwords).
- Prefer structured logs for key events.

## Testing policy

- Web layer: `WebTestClient` 기반 통합 테스트 우선
- Domain/Application: 순수 단위 테스트 우선
- Kafka 연동은 테스트컨테이너/임베디드 사용 여부는 프로젝트 표준을 따른다.

## Data access rules

- Repositories/adapters must return Mono/Flux only.
- No hidden side effects in `map`.
- Keep queries and persistence logic inside infrastructure/adapters; domain remains pure.
- Persistence Entity는 infrastructure 내부 모델이다. application/domain 바깥으로 노출하지 않는다.
- Domain ↔ Entity 매핑 책임은 infrastructure에 둔다(어댑터 또는 전용 mapper).
- SRP를 위해 어댑터 책임을 분리한다.
    - 쓰기 포트 구현(`...Appender`)과 읽기 포트 구현(`...Reader`)은 분리 권장
    - 클래스명은 실제 책임과 일치해야 한다.

## Adding a new API endpoint (checklist)

1) Add route:
    - `...presentation.router...` (or router config module)
2) Add handler:
    - `...presentation.handler...`
3) Add application use-case/service:
    - `...application...`
4) Add/adjust domain model if needed:
    - `...domain...`
5) Add tests:
    - Web layer: `WebTestClient`
    - Unit tests for domain/application logic
6) Run:
    - `./gradlew test` and `./gradlew check`

### Reactive performance rules (optional)

- Avoid unbounded concurrency:
    - `flatMap` 사용 시 concurrency를 제한하거나, 처리량이 큰 경우 배치/윈도우링을 고려한다.
- Do not collect large streams into memory:
    - `collectList()`는 데이터 규모가 작을 때만 사용한다.

## Safe change policy (agent guidance)

- Keep diffs minimal and localized.
- Prefer extending existing patterns over introducing new frameworks.
- If repository conventions conflict with a suggestion, repository conventions win.
- Always ensure build/tests pass via CLI commands above.
