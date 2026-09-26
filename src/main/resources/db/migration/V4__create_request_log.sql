-- Lightweight request log for rate limiting and later analytics. client_id is a
-- hashed IP, never a raw address.
CREATE TABLE request_log (
    id         BIGSERIAL PRIMARY KEY,
    client_id  TEXT NOT NULL,
    path       TEXT NOT NULL,
    status     INTEGER,
    created_at TIMESTAMPTZ NOT NULL
);

-- Rate-limit lookup: recent requests per client.
CREATE INDEX idx_request_log_client_time ON request_log (client_id, created_at);
