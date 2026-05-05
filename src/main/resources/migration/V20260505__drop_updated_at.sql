-- Drop updatedAt column from all tables
--
-- 영향 테이블: users, user_sessions, protects, protect_target_indexing_outbox, audits, strategy_chat_rooms, strategy_chat_messages
--
-- 적용 환경: 기존 데이터를 가진 dev / prod
-- 신규 환경(K8s db-init-job)은 schema.sql만으로 충분하므로 이 스크립트 불필요
--
-- 배경: BaseEntity의 @LastModifiedDate updatedAt은 @EnableR2dbcAuditing이 없어
--        INSERT 시 DB DEFAULT로만 채워지고 UPDATE 시 갱신되지 않는 stale 컬럼.
--        활성화 대신 컬럼 자체를 제거한다.
--
-- IF EXISTS 사용으로 idempotent (재실행 안전)

ALTER TABLE users DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE user_sessions DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE protects DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE protect_target_indexing_outbox DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE audits DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE strategy_chat_rooms DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE strategy_chat_messages DROP COLUMN IF EXISTS "updatedAt";
