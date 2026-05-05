-- refresh_token 평문 저장을 SHA-256 해시 저장으로 전환
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS refresh_token_hash CHAR(64);
UPDATE user_sessions
SET refresh_token_hash = encode(sha256(refresh_token::bytea), 'hex')
WHERE refresh_token_hash IS NULL AND refresh_token IS NOT NULL;
ALTER TABLE user_sessions ALTER COLUMN refresh_token_hash SET NOT NULL;
ALTER TABLE user_sessions DROP COLUMN IF EXISTS refresh_token;
