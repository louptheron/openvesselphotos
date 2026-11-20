-- The company owning a vessel (armement), e.g. France Pélagique, and its group, e.g. Cornelis Vrolijk: shown on the ports
-- site for the industrial fleets. From public sources, each cited; replaced by each import.
CREATE TABLE vessel_owners (
    cfr         VARCHAR(12) PRIMARY KEY,
    owner       VARCHAR(100) NOT NULL,
    owner_group VARCHAR(100),
    source_url  VARCHAR(500) NOT NULL
);
