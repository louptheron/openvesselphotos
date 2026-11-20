-- Fishing ports, by UN/LOCODE (e.g. FRQUY for Erquy, or FRLRH-0015 for a terminal of La Rochelle). The position is set apart, by an import of port positions.
CREATE TABLE ports (
    locode    VARCHAR(10) PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    -- ISO 3166-1 alpha-2, e.g. FR
    country   VARCHAR(2) NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

-- The port each vessel is based at, observed from its positions: replaced by each import.
-- Not linked to vessels: the vessel may not be known by Open Vessel Photos yet, and is matched by its CFR.
CREATE TABLE vessel_home_ports (
    cfr                  VARCHAR(12) PRIMARY KEY,
    vessel_name          VARCHAR(100),
    port_locode          VARCHAR(10) NOT NULL REFERENCES ports (locode),
    observations_total   INTEGER NOT NULL,
    observations_at_port INTEGER NOT NULL,
    mean_distance_km     NUMERIC(8, 2),
    imported_on          DATE NOT NULL
);

CREATE INDEX vessel_home_ports_port_locode ON vessel_home_ports (port_locode);
