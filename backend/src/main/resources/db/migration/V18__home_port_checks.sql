-- The imported home ports checked against the port visits of the vessels on Global Fishing Watch: the port each
-- visited most in the last year, compared to its imported home port. Replaced for a vessel at each check.
CREATE TABLE home_port_checks (
    cfr                  VARCHAR(12) PRIMARY KEY,
    imported_locode      VARCHAR(12) NOT NULL,
    result               VARCHAR(20) NOT NULL,
    -- The port of the map the vessel visited most, when on the map
    gfw_locode           VARCHAR(12),
    -- As named by Global Fishing Watch, e.g. for a port not on the map
    gfw_port_name        VARCHAR(200),
    visits_total         INTEGER NOT NULL,
    visits_at_gfw_port   INTEGER NOT NULL,
    visits_at_imported   INTEGER NOT NULL,
    -- Between the imported home port and the port visited most
    distance_km          NUMERIC(8, 1),
    checked_on           DATE NOT NULL
);
