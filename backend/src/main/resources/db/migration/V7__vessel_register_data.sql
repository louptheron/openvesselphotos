-- Technical data of the EU fleet register, replaced by each import of the register.
-- Gears and vessel types are FAO codes (ISSCFG, ISSCFV), e.g. OTB for bottom otter trawls.
ALTER TABLE vessels ADD COLUMN vessel_type VARCHAR(5);
ALTER TABLE vessels ADD COLUMN year_built INTEGER;
ALTER TABLE vessels ADD COLUMN length_m NUMERIC(6, 2);
ALTER TABLE vessels ADD COLUMN engine_power_kw NUMERIC(8, 2);
ALTER TABLE vessels ADD COLUMN tonnage_gt NUMERIC(9, 2);
ALTER TABLE vessels ADD COLUMN main_gear VARCHAR(5);
-- Comma-separated, e.g. "LLS,GNS"
ALTER TABLE vessels ADD COLUMN other_gears VARCHAR(40);
-- International radio call sign (IRCS)
ALTER TABLE vessels ADD COLUMN call_sign VARCHAR(10);
