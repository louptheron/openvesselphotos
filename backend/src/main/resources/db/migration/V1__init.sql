-- A vessel is identified by its CFR (EU fleet register), its MMSI (radio identity), or both.
CREATE TABLE vessels (
    id         SERIAL PRIMARY KEY,
    cfr        VARCHAR(12) UNIQUE,
    mmsi       VARCHAR(9) UNIQUE,
    name       VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT vessels_has_a_key CHECK (cfr IS NOT NULL OR mmsi IS NOT NULL)
);

CREATE INDEX vessels_created_at_idx ON vessels (created_at DESC);

-- A photo is submitted under one key. `vessel_id` stays NULL until approval when the key is unknown.
-- `number` (the public `n` in /photos/{n}.jpg) is set on approval and never reused.
CREATE TABLE photos (
    id                    SERIAL PRIMARY KEY,
    vessel_id             INTEGER REFERENCES vessels (id),
    number                INTEGER,
    status                VARCHAR(16)  NOT NULL,
    submitted_key_type    VARCHAR(4)   NOT NULL,
    submitted_key         VARCHAR(12)  NOT NULL,
    submitted_vessel_name VARCHAR(100),
    credit                VARCHAR(100) NOT NULL,
    licence               VARCHAR(16)  NOT NULL,
    taken_on              DATE,
    place                 VARCHAR(200),
    contributor_email     VARCHAR(254),
    width                 INTEGER      NOT NULL,
    height                INTEGER      NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at           TIMESTAMPTZ,
    reject_reason         VARCHAR(200),
    CONSTRAINT photos_unique_number UNIQUE (vessel_id, number)
);

CREATE INDEX photos_status_idx ON photos (status);

-- Image bytes, one row per size. Kept out of `photos` so listing photos never loads them.
CREATE TABLE photo_files (
    photo_id INTEGER     NOT NULL REFERENCES photos (id) ON DELETE CASCADE,
    size     VARCHAR(8)  NOT NULL,
    content  BYTEA       NOT NULL,
    PRIMARY KEY (photo_id, size)
);

-- Suggestion to add the missing key to a vessel.
CREATE TABLE key_link_requests (
    id          SERIAL PRIMARY KEY,
    vessel_id   INTEGER     NOT NULL REFERENCES vessels (id),
    key_type    VARCHAR(4)  NOT NULL,
    key_value   VARCHAR(12) NOT NULL,
    contributor VARCHAR(100),
    status      VARCHAR(16) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ
);

CREATE TABLE reports (
    id          SERIAL PRIMARY KEY,
    photo_id    INTEGER      NOT NULL REFERENCES photos (id),
    reason      VARCHAR(32)  NOT NULL,
    message     VARCHAR(1000),
    status      VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ
);
