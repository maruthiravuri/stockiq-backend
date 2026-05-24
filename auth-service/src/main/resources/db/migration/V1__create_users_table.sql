CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(100) NOT NULL UNIQUE,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ANALYST',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- Seed admin user (password: Admin@123, bcrypt 10 rounds, $2a$ Spring-compatible)
INSERT INTO users (email, username, password_hash, role)
VALUES ('admin@stockiq.com', 'admin',
        '$2a$10$.cZApRs/4sHcvy41LCRyV.znF3ubydLfsKxThxFjFk5vpQWT8HZo2',
        'ADMIN');
