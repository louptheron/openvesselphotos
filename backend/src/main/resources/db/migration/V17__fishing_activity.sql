-- The vessels of the ports site on Global Fishing Watch: their identifiers there (comma-separated, empty when not found,
-- e.g. without AIS), looked up again after a while.
CREATE TABLE gfw_vessels (
    cfr          VARCHAR(12) PRIMARY KEY,
    vessel_ids   VARCHAR(2000) NOT NULL,
    looked_up_on DATE NOT NULL
);

-- Where each vessel fished over the last 12 months: hours of fishing by cell of 0.05° (about 5 km), from its fishing
-- events on Global Fishing Watch. Replaced for a vessel at each synchronisation.
CREATE TABLE vessel_fishing_cells (
    cfr       VARCHAR(12) NOT NULL,
    latitude  NUMERIC(6, 3) NOT NULL,
    longitude NUMERIC(7, 3) NOT NULL,
    hours     NUMERIC(9, 2) NOT NULL,
    PRIMARY KEY (cfr, latitude, longitude)
);
