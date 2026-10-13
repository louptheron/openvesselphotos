-- API keys for POST requests to the public API. Only the SHA-256 hash of a key is stored.
CREATE TABLE api_keys (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    email        VARCHAR(254)  NOT NULL,
    organisation VARCHAR(200),
    intended_use VARCHAR(1000) NOT NULL,
    status       VARCHAR(16)   NOT NULL,
    key_hash     CHAR(64) UNIQUE,
    -- First characters of the key, to recognize it in the admin
    key_prefix   VARCHAR(12),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    reviewed_at  TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ
);

ALTER TABLE photos ADD COLUMN api_key_id INTEGER REFERENCES api_keys (id);
