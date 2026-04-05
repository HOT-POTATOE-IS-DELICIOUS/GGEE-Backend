CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,
    INDEX idx_user_sessions_user_id (user_id),
    UNIQUE INDEX idx_user_sessions_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
