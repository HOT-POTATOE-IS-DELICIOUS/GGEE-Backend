-- One-time backfill: protect_target/protect_target_info를 users 테이블에서 protects 테이블로 이전
--
-- 적용 환경: 기존 데이터를 가진 dev / prod
-- 신규 환경(K8s db-init-job)은 schema.sql만으로 충분하므로 이 스크립트 불필요
--
-- 적용 순서 (반드시 단일 트랜잭션):
--   1) protects 테이블이 없다면 schema.sql의 CREATE TABLE 문을 먼저 실행
--   2) 본 스크립트를 psql 등으로 실행
--   3) 신규 코드 배포
--
-- 롤백 시:
--   - 신규 코드를 되돌린 뒤, 스크립트 마지막의 ALTER TABLE을 되돌리면 됨
--   - protects 테이블은 유지(데이터 손실 방지)

BEGIN;

-- 1. 활성 user의 protect_target을 protects 테이블로 복사
--    user.id를 protect.id로 재사용 (1:1 매핑이라 안전, 신규 ID 발급 불필요)
INSERT INTO protects (id, user_id, target, info, "createdAt", "updatedAt", deleted, deleted_at)
SELECT
    u.id,
    u.id,
    u.protect_target,
    u.protect_target_info,
    u."createdAt",
    u."updatedAt",
    false,
    NULL
FROM users u
WHERE u.deleted = false
  AND u.protect_target IS NOT NULL
  AND u.protect_target_info IS NOT NULL
ON CONFLICT (id) DO NOTHING;

-- 2. 검증: 활성 user 수와 활성 protect 수 일치 확인
DO $$
DECLARE
    active_user_count BIGINT;
    active_protect_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO active_user_count FROM users WHERE deleted = false;
    SELECT COUNT(*) INTO active_protect_count FROM protects WHERE deleted = false;
    IF active_user_count <> active_protect_count THEN
        RAISE EXCEPTION 'backfill 검증 실패: active users=%, active protects=%',
            active_user_count, active_protect_count;
    END IF;
END $$;

-- 3. users 테이블에서 컬럼 제거
ALTER TABLE users DROP COLUMN IF EXISTS protect_target;
ALTER TABLE users DROP COLUMN IF EXISTS protect_target_info;

COMMIT;
