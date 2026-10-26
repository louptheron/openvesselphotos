-- What Wikidata knows of a vessel, found by its MMSI; replaced by each Wikidata import.
ALTER TABLE vessels ADD COLUMN wikidata_id VARCHAR(16);
ALTER TABLE vessels ADD COLUMN imo VARCHAR(7);
ALTER TABLE vessels ADD COLUMN wikidata_ship_type_en VARCHAR(100);
ALTER TABLE vessels ADD COLUMN wikidata_ship_type_fr VARCHAR(100);
ALTER TABLE vessels ADD COLUMN wikidata_year_built INTEGER;
ALTER TABLE vessels ADD COLUMN wikidata_length_m NUMERIC(6, 2);
-- Other names of the vessel, e.g. a former name; separated by "|"
ALTER TABLE vessels ADD COLUMN other_names VARCHAR(500);
ALTER TABLE vessels ADD COLUMN wikidata_imported_on DATE;
