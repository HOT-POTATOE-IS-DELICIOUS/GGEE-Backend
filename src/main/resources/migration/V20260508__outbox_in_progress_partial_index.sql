-- recoverStaleClaimed 쿼리(`status='IN_PROGRESS' AND claimed_at < cutoff AND deleted=false`)를
-- 위한 partial index. IN_PROGRESS 상태 행만 인덱스에 들어가므로 일반 트래픽에서는 유지비가 거의 없고,
-- 장애 복구 시 회수 비용이 (status, "createdAt") 인덱스보다 훨씬 작아진다.

CREATE INDEX IF NOT EXISTS idx_outbox_in_progress_claimed_at
    ON protect_target_indexing_outbox (claimed_at)
    WHERE status = 'IN_PROGRESS' AND deleted = false;
