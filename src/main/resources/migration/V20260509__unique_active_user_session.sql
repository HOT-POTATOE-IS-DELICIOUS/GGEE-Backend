-- 한 사용자당 활성 세션은 하나만 유지한다.
-- 기존 중복 활성 세션이 있으면 가장 최근 세션만 남기고 나머지는 비활성화한 뒤 partial unique index를 생성한다.

WITH ranked_sessions AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY "createdAt" DESC, id DESC
        ) AS rn
    FROM user_sessions
    WHERE deleted = false
)
UPDATE user_sessions us
SET deleted = true,
    deleted_at = CURRENT_TIMESTAMP
FROM ranked_sessions ranked
WHERE us.id = ranked.id
  AND ranked.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uniq_user_sessions_user_active
    ON user_sessions (user_id) WHERE deleted = false;
