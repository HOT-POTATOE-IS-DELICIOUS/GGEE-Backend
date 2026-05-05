# 운영 마이그레이션

신규 환경은 `src/main/resources/schema.sql`로 자동 초기화되므로 이 디렉터리 스크립트는 **기존 데이터를 가진 환경(dev/prod)** 에만 필요합니다.

## 파일 명명 규약

```
V<YYYYMMDD>__<설명>.sql
```

- 본 프로젝트는 Flyway 등의 자동 마이그레이션 도구를 도입하지 않았습니다.
- 명명 규약은 향후 도구 도입 시 호환을 위해 Flyway 컨벤션을 따릅니다.

## 적용 절차

```bash
# 운영 DB에 직접 적용
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f <스크립트명>.sql

# 컨테이너 환경
docker exec -i <postgres> psql -U <user> -d <db> -v ON_ERROR_STOP=1 \
  < <스크립트명>.sql
```

`-v ON_ERROR_STOP=1`로 검증 실패 시 트랜잭션이 롤백됩니다.

## 적용 순서 (배포와 함께)

1. 새 코드의 `schema.sql`이 정의한 신규 테이블(`protects` 등)을 먼저 만든다 (CREATE TABLE은 IF NOT EXISTS이므로 운영에서 직접 실행해도 안전).
2. 본 디렉터리의 마이그레이션 스크립트를 적용한다.
3. 새 코드를 배포한다.

순서를 뒤집으면 새 코드가 존재하지 않는 컬럼/테이블을 참조해 즉시 500이 발생합니다.

---

## V20260504__split_protect_from_users.sql

**배경**: `users`에 있던 `protect_target`, `protect_target_info` 컬럼을 별도의 `protects` 도메인 테이블로 분리.

**영향**: `users` 테이블에서 두 컬럼 제거 + `protects` 테이블에 백필.

## V20260505__drop_updated_at.sql

**배경**: `BaseEntity`의 `@LastModifiedDate updatedAt` 컬럼은 `@EnableR2dbcAuditing`이 없어 INSERT 시 DB DEFAULT로만 채워지고 UPDATE 시 갱신되지 않는 stale 컬럼이었음. 활성화 대신 컬럼 자체를 제거.

**영향 테이블**: `users`, `user_sessions`, `protect_target_indexing_outbox`, `audits`

**적용 방법**:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f V20260505__drop_updated_at.sql
```

`IF EXISTS`를 사용하므로 idempotent — 재실행해도 안전합니다.
