-- The updates of the data of the ports site, by source, for its public changelog (/mises-a-jour) and the dates of its pages
CREATE TABLE data_updates (
    id         BIGSERIAL PRIMARY KEY,
    source     VARCHAR(30) NOT NULL,
    updated_on DATE NOT NULL,
    -- In French, shown as it is, e.g. "Ventes 2025 de 38 criées"
    summary    VARCHAR(300) NOT NULL
);

CREATE INDEX data_updates_updated_on ON data_updates (updated_on DESC);

-- The home ports imported before this table
INSERT INTO data_updates (source, updated_on, summary)
SELECT 'HOME_PORTS', max(imported_on), 'Ports d’attache de ' || count(*) || ' navires'
FROM vessel_home_ports
HAVING count(*) > 0;
