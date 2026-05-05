-- protect_target_indexing_outbox에 claimed_at 컬럼을 추가하여
-- IN_PROGRESS 상태로 멈춘 outbox를 부팅 시 회수할 수 있게 한다.

ALTER TABLE protect_target_indexing_outbox
    ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP NULL;
