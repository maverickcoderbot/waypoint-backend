-- Cached upstream responses (Overpass, geocoding, elevation, photos).
CREATE TABLE api_cache (
    cache_key  TEXT PRIMARY KEY,
    payload    JSONB NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

-- Sweep of expired rows filters on expires_at.
CREATE INDEX idx_api_cache_expires_at ON api_cache (expires_at);
