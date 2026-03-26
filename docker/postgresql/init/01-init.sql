CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS perf_comments (
  comment_key  VARCHAR(512) PRIMARY KEY,
  site         VARCHAR(64)  NOT NULL,
  keyword      VARCHAR(128) NOT NULL,
  post_url     VARCHAR(512) NOT NULL,
  comment_id   INT          NOT NULL,
  author       VARCHAR(128),
  content      TEXT,
  likes        VARCHAR(16),
  dislikes     VARCHAR(16),
  crawled_at   VARCHAR(64)
);
