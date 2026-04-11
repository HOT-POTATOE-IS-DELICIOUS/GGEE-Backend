# AGENTS.md

## 저장소 개요

- 프로젝트:
    - Spring Boot + Spring WebFlux 기반 Java 백엔드 프로젝트 (단일 모듈)
- 한 줄 소개:
    - 공개 전 콘텐츠를 다중 사회·이념 관점에서 자동 검수해 평판 리스크를 조기 탐지·완화하고, 공개 후 여론 전개를 모니터링·역추적하는 브랜드 평판 방화벽 플랫폼 백엔드.
- 에이전트 목표:
    - CLI에서 빠르게 구조를 파악하고, 변경 범위를 최소화하며, 팀 규칙에 맞는 고품질 수정/기능 추가를 수행한다.
- 기술 스택:
    - Java 21
    - Spring Boot 4.0.2 / WebFlux (Reactive)
    - Gradle (단일 모듈)
    - R2DBC MySQL (io.asyncer:r2dbc-mysql)
    - Kafka (spring-kafka)
    - JWT (io.jsonwebtoken:jjwt 0.13.0)
    - Snowflake ID (자체 구현)
    - Lombok

## 먼저 읽을 것 (탐색 가이드)

- 우선 확인:
    - `build.gradle` (단일 모듈 - subproject 없음)
    - `team.hotpotato.security.SecurityConfig` (인증/인가)
    - `team.hotpotato.support.advice.ApiControllerAdvice` (글로벌 에러 핸들링)
    - `team.hotpotato.domain.{domain}.api.*Controller` (Web 진입점)
    - `team.hotpotato.infrastructure.r2dbc.*` (영속성 어댑터)
- 검색 키워드:
    - `@RestController`, `@RequestMapping`, `SecurityWebFilterChain`, `@RestControllerAdvice`
    - `TransactionalOperator`, `R2dbcEntityTemplate`, `ReactiveCrudRepository`
    - `AuthFilter`, `TokenGeneratorAdapter`, `TokenResolverAdapter`

## Package map

단일 모듈, 루트 패키지: `team.hotpotato`

```
team.hotpotato
├── GgeeBackendApplication.java          // Spring Boot 진입점
│
├── common/                              // 공유 타입 (전 계층 사용 가능)
│   ├── exception/
│   │   ├── BusinessBaseException.java   // 도메인 예외 베이스 클래스
│   │   └── ErrorCode.java               // 에러 코드 enum
│   └── identity/
│       └── IdGenerator.java             // ID 생성 포트 (인터페이스)
│
├── domain/{domain}/                     // 도메인별 수직 슬라이스
│   ├── api/                             // Web 계층 (Controller, Request, Response DTO)
│   ├── application/
│   │   ├── auth/                        // 인증 관련 포트 (TokenGenerator, TokenResolver 등)
│   │   ├── dto/                         // command / result DTO
│   │   ├── persistence/                 // 영속성 포트
│   │   └── usecase/                     // 유스케이스 인터페이스 + 구현체, 도메인 예외
│   └── domain/                          // 순수 도메인 모델 (Spring 의존 없음)
│
├── infrastructure/                      // 외부 기술 어댑터
│   ├── common/                          // 공통 설정 (PropertiesConfig 등)
│   ├── event/                           // Kafka 이벤트 발행 어댑터
│   ├── jwt/                             // JWT 어댑터 구현체 + 예외 + 설정
│   ├── kafka/                           // Kafka 설정
│   ├── r2dbc/
│   │   ├── common/                      // BaseEntity 등
│   │   └── {domain}/                    // Entity, Mapper, RepositoryAdapter
│   └── snowflake/                       // Snowflake ID 구현체 + 설정
│
├── security/                            // Spring Security 설정
│   ├── SecurityConfig.java              // SecurityWebFilterChain 빈
│   ├── AuthFilter.java                  // JWT 인증 필터 (WebFilter)
│   ├── CorsConfig.java
│   └── CorsProperties.java
│
└── support/                             // 횡단 관심사
    ├── advice/
    │   ├── ApiControllerAdvice.java     // @RestControllerAdvice (글로벌 에러 핸들링)
    │   └── ErrorCodeHttpStatusMapper.java
    └── logging/
        └── ExceptionLogger.java
```

### 테스트 패키지

```
src/test/java/team/hotpotato/
├── api/controller/                      // WebTestClient 기반 통합 테스트
├── application/usecase/                 // 순수 단위 테스트
└── bootstrap/                           // 애플리케이션 컨텍스트 로드 테스트
```

## 실행 방법 (CLI)

- 의존 서비스 실행:
    - `docker compose up -d` (MySQL, Kafka)
- 애플리케이션 실행:
    - `./gradlew bootRun`
- 테스트 실행:
    - `./gradlew test`
- 전체 검증 실행:
    - `./gradlew check`

## 코드 규칙 (최우선)

### 네이밍 / DTO 규칙 (엄격)

- 계층별 DTO 네이밍 규칙:
    - API 계층 (`domain/{domain}/api/`): `request / response`
      (예: `LoginRequest`, `LoginResponse`)
    - Application 계층 (`domain/{domain}/application/dto/`): `command / result`
      (예: `LoginCommand`, `LoginResult`)
- Application 계층에 `Request/Response` 네이밍 DTO를 새로 만들지 않는다.
- Controller에서 `request → command`, `result → response`로 직접 매핑한다. API DTO를 Application 계층으로 넘기지 않는다.
- 클래스명은 책임 중심으로 짓고 기술 종속 네이밍을 피한다.
  (예: domain/application에서 `R2dbc`, `Jwt`, `MySql` 같은 기술명을 타입명에 노출하지 않는다)

### 패키지 규칙 (엄격)

- 신규 코드에서 `team.hotpotato.ggeebackend...` 패키지는 사용하지 않는다.
- 파일 경로와 `package` 선언은 항상 일치해야 한다.
- `domain/{domain}/domain/` 패키지는 Spring 의존성을 가지지 않는다.
- Persistence Entity는 `infrastructure/r2dbc/` 내부 모델이다. application/domain 바깥으로 노출하지 않는다.
- Domain ↔ Entity 매핑 책임은 `infrastructure`의 Mapper 클래스에 둔다.

### Reactive (WebFlux) 규칙

- 운영 코드에서 `block()` 또는 `subscribe()`를 호출하지 않는다.
    - 허용 범위: 테스트 / 일회성 로컬 CLI 유틸리티(명시적으로 문서화된 경우만)
- event-loop 스레드에서 blocking I/O를 수행하지 않는다.
    - 불가피하면 `boundedElastic` 또는 전용 scheduler를 사용하고 이유를 남긴다.
- 연산자는 의미에 맞게 사용한다.
    - `map`: 순수 변환
    - `flatMap`: 비동기 경계
    - `then`: 이전 값이 필요 없는 순차 실행
    - 부수효과: 로깅 용도로만 `doOnNext/doOnSuccess/doOnError`를 우선 사용한다(상태 변경 금지).

### 트랜잭션 규칙 (엄격)

- 트랜잭션은 기본값이 아니다. 무분별한 트랜잭션 사용을 금지한다.
    - 단일 insert/update/delete 1회, 단순 CRUD, read-only 흐름에는 트랜잭션을 사용하지 않는다.
    - 트랜잭션은 리소스/락/컨텍스트 비용이 있으므로 "필요한 곳에만 최소 범위로" 적용한다.

- 원자성(atomicity)이 반드시 필요한 경우에만 트랜잭션을 사용한다.
    - "A가 실패하면 B도 반드시 롤백"되어야 하는 원자성(all-or-nothing)이 요구될 때만 트랜잭션을 사용한다.
    - 예시 (트랜잭션 필요):
        - 2개 이상 DB write가 하나의 업무 단위로 묶여야 함
          (예: 주문 생성 + 재고 차감 + 결제 상태 기록)
        - 비즈니스 불변식(invariant)을 지키기 위한 다중 쓰기
          (예: 포인트 차감 + 이력 적재)

- 예시 (트랜잭션 불필요):
    - 단일 저장/업데이트 1회
    - 조회-only
    - 캐시 갱신, 로그 적재, 알림 발송, 메시지 발행 같은 부수효과
      → 실패해도 본 DB 작업을 롤백시키면 안 되는 작업은 트랜잭션 밖으로 분리
      (이벤트/비동기/아웃박스 패턴 등 고려)

- Reactive 트랜잭션 작성 방식:
    - R2DBC/WebFlux에서는 `TransactionalOperator`를 사용한다.
    - DB 트랜잭션 안에 원격 호출(HTTP), 메시지 publish(Kafka), 파일 I/O를 포함하지 않는다.
    - 트랜잭션 경계는 "원자성이 필요한 DB write"까지만 최소화한다.

- 롤백 정책:
    - 트랜잭션 파이프라인에서 에러가 발생하면 error 신호가 전파되어 롤백되도록 작성한다.
    - 트랜잭션 내부에서 `onErrorResume`로 에러를 삼키지 않는다(명시적 보상 트랜잭션 제외).

- 트랜잭션 추가 전 체크리스트:
    1) 2개 이상 DB write가 있으며 all-or-nothing이 반드시 필요하다.
    2) 부분 성공이 비즈니스 불변식을 깨거나 잘못된 상태를 외부에 노출한다.
    3) 트랜잭션 경계에 원격 호출/메시징/블로킹 I/O가 포함되지 않는다.

### 에러 처리 및 검증

- 전역 에러 처리: `team.hotpotato.support.advice.ApiControllerAdvice` (`@RestControllerAdvice`)
    - `BusinessBaseException` → `ErrorCode`로 HTTP 상태 매핑 (`ErrorCodeHttpStatusMapper`)
    - `WebExchangeBindException` → 400 Bad Request (Bean Validation 실패)
    - `Exception` → 500 Internal Server Error
- 도메인 예외:
    - `BusinessBaseException`을 상속한 의미 있는 타입으로 분리한다.
    - HTTP 응답 매핑은 `ApiControllerAdvice` 한 곳에서만 처리한다.
- 검증:
    - Bean Validation (`@Valid`) 사용. 컨트롤러 메서드 파라미터에 적용한다.

### 로깅 / 관측성

- `ExceptionLogger`를 통해 예외 로그를 남긴다.
- 비밀값(tokens/passwords)은 로그에 남기지 않는다.
- 주요 이벤트는 구조화된 로그를 우선 사용한다.

## 테스트 정책

- Web 계층: `WebTestClient` 기반 통합 테스트 (`src/test/.../api/controller/`)
- Domain/Application 계층: 순수 단위 테스트 (`src/test/.../application/usecase/`)
- Kafka 연동은 테스트컨테이너/임베디드 사용 여부는 프로젝트 표준을 따른다.

## 데이터 접근 규칙

- Repository/adapter는 반드시 `Mono`/`Flux`만 반환한다.
- `map` 안에 숨은 부수효과를 넣지 않는다.
- 쿼리와 영속성 로직은 `infrastructure/r2dbc/` 내부에 유지하고, domain은 순수하게 유지한다.
- Persistence Entity는 `infrastructure/r2dbc/{domain}/` 내부 모델이다. 바깥으로 노출하지 않는다.
- Domain ↔ Entity 매핑 책임은 `{Domain}EntityMapper`에 둔다.
- SRP를 위해 어댑터 책임은 명확히 유지한다.
    - 영속성 구현은 현재 코드베이스의 `...RepositoryAdapter` 패턴을 우선 따른다.

## 새 API endpoint 추가 체크리스트

1) Controller와 request/response DTO 추가:
    - `domain/{domain}/api/{Domain}Controller.java`
    - `domain/{domain}/api/{Xxx}Request.java`, `{Xxx}Response.java`
2) use-case 인터페이스와 구현체 추가:
    - `domain/{domain}/application/usecase/{UseCase}.java` (interface)
    - `domain/{domain}/application/usecase/{UseCase}UseCase.java` (impl)
3) command/result DTO 추가:
    - `domain/{domain}/application/dto/{Xxx}Command.java`, `{Xxx}Result.java`
4) 필요 시 domain 모델 추가/수정:
    - `domain/{domain}/domain/`
5) 필요 시 영속성 포트와 adapter 추가:
    - Port: `domain/{domain}/application/output/{Domain}Repository.java`
    - Adapter: `infrastructure/r2dbc/{domain}/{Domain}RepositoryAdapter.java`
6) 테스트 추가:
    - Web layer: `WebTestClient` (`src/test/.../api/controller/`)
    - Unit tests: `src/test/.../application/usecase/`
7) 실행:
    - `./gradlew test` and `./gradlew check`

## 보안 (AuthFilter)

- `team.hotpotato.security.AuthFilter`: WebFilter로 JWT를 검증하고 SecurityContext에 인증 정보를 주입한다.
- 공개 경로: `/auth/**`, `/actuator/health`, `/actuator/info`
- 그 외 모든 경로는 인증 필요 (`anyExchange().authenticated()`)
- JWT 어댑터: `infrastructure/jwt/TokenGeneratorAdapter`, `TokenResolverAdapter`, `TokenValidatorAdapter`

## 안전한 변경 정책 (agent guidance)

- diff는 최소화하고 변경 범위는 국소적으로 유지한다.
- 무조건 기존 코드베이스의 구조, 패턴, 아키텍처를 따른다.
- 새 프레임워크를 도입하기보다 기존 패턴을 확장하는 방식을 우선한다.
- 제안보다 저장소 규칙이 우선한다.
- 위 CLI 명령으로 build/tests 통과를 항상 확인한다.

## 커밋 규칙

- 사용자가 커밋을 요청하면 반드시 `COMMIT.md`를 먼저 확인하고 그 규칙을 따른다.
- 커밋 메시지는 반드시 한국어로 작성한다.
- 사용자가 명시적으로 요청하지 않으면 `commit`, `amend`, `push`를 수행하지 않는다.
