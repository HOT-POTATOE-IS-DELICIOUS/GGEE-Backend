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
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f V20260504__split_protect_from_users.sql

# 컨테이너 환경
docker exec -i <postgres> psql -U <user> -d <db> -v ON_ERROR_STOP=1 \
  < V20260504__split_protect_from_users.sql
```

`-v ON_ERROR_STOP=1`로 검증 실패 시 트랜잭션이 롤백됩니다.

## 적용 순서 (배포와 함께)

1. 새 코드의 `schema.sql`이 정의한 신규 테이블(`protects` 등)을 먼저 만든다 (CREATE TABLE은 IF NOT EXISTS이므로 운영에서 직접 실행해도 안전).
2. 본 디렉터리의 마이그레이션 스크립트를 적용한다.
3. 새 코드를 배포한다.

순서를 뒤집으면 새 코드가 존재하지 않는 컬럼/테이블을 참조해 즉시 500이 발생합니다.
