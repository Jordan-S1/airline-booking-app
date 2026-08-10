-- Password reset tokens.
--
-- Only a hash of the token is stored. The plaintext exists once, in the reply
-- to the request that created it, and is never persisted — so a leak of this
-- table does not let the reader reset anyone's password, exactly as storing
-- password hashes rather than passwords protects the users table.
--
-- Tokens are single use (used_at) and short lived (expires_at). Rows are kept
-- after use rather than deleted, so a token cannot be replayed and so a
-- support question about a suspicious reset has something to look at.

CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Redeeming a token looks it up by hash; this is the only read path that matters.
CREATE INDEX idx_password_reset_tokens_hash ON password_reset_tokens (token_hash);

-- Issuing a new token invalidates a user's outstanding ones, which reads by user.
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
