-- A home port suggested by a visitor for a vessel, by its CFR, reviewed by the admin. Once approved, it is the vessel's
-- home port (vessel_home_ports.source = 'MANUAL'), kept over the imports and the ports found by the other sources.
CREATE TABLE home_port_corrections (
    id          SERIAL PRIMARY KEY,
    cfr         VARCHAR(12)  NOT NULL,
    port_locode VARCHAR(10)  NOT NULL REFERENCES ports (locode),
    comment     VARCHAR(500),
    contributor VARCHAR(100),
    status      VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ
);

CREATE INDEX home_port_corrections_status ON home_port_corrections (status);
