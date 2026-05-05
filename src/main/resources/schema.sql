CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
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

CREATE TABLE IF NOT EXISTS protects (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target VARCHAR(255) NOT NULL,
    info VARCHAR(255) NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_protects_user_id ON protects (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uniq_protects_user_active
    ON protects (user_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_protects_target_info_active
    ON protects (target, info) WHERE deleted = false;

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
    reviews_json TEXT NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_audits_user_id_created_at
    ON audits (user_id, "createdAt");

CREATE TABLE IF NOT EXISTS strategy_chat_rooms (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(20) NOT NULL,
    last_chatted_at TIMESTAMP NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_strategy_chat_rooms_user_id
    ON strategy_chat_rooms (user_id, "createdAt" DESC);

CREATE TABLE IF NOT EXISTS strategy_chat_messages (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    intent VARCHAR(32) NULL,
    refined_query VARCHAR(512) NULL,
    meta_json JSON NULL,
    ai_message_id VARCHAR(32) NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_strategy_chat_messages_room_id
    ON strategy_chat_messages (room_id, "createdAt" ASC);
