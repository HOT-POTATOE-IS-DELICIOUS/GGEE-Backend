CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  protect_target VARCHAR(255) NOT NULL,
  protect_target_info VARCHAR(255) NOT NULL,
  "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions (user_id);

CREATE TABLE IF NOT EXISTS protect_target_indexing_outbox (
    id BIGINT PRIMARY KEY,
    protect_target VARCHAR(255) NOT NULL,
    protect_target_info VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    published_at TIMESTAMP NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_protect_target_indexing_outbox_status_created_at
    ON protect_target_indexing_outbox (status, "createdAt");

CREATE TABLE IF NOT EXISTS audits (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    protect_target VARCHAR(255) NOT NULL,
    protect_target_info VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    reviews_json TEXT NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_audits_user_id_created_at
    ON audits (user_id, "createdAt");
