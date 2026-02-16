CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  INDEX idx_users_email (email)
);
