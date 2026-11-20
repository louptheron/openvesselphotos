-- Sales declared at the fish auctions (halles à marée, criées), by species, from FranceAgriMer's yearly workbook
-- "Ventes déclarées en halles à marée": replaced by each import. species is NULL for the total of all species.
CREATE TABLE auction_sales (
    auction     VARCHAR(100) NOT NULL,
    -- The port of the auction, matched by name at import; NULL when no port matched
    port_locode VARCHAR(10),
    year        INTEGER NOT NULL,
    species     VARCHAR(100),
    quantity_t  NUMERIC(12, 3) NOT NULL,
    value_keur  NUMERIC(14, 3) NOT NULL
);

CREATE INDEX auction_sales_port_locode ON auction_sales (port_locode);

-- Monthly sales of the main species in all French auctions, for their seasons
CREATE TABLE species_monthly_sales (
    species    VARCHAR(100) NOT NULL,
    year       INTEGER NOT NULL,
    month      SMALLINT NOT NULL,
    quantity_t NUMERIC(12, 3) NOT NULL,
    PRIMARY KEY (species, year, month)
);
