-- Where a vessel's home port comes from: the imported observations (IMPORT), or its port visits on Global Fishing Watch
-- (GFW) for the vessels of the EU fleet register without one. An import replaces its own rows only.
ALTER TABLE vessel_home_ports ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'IMPORT';
