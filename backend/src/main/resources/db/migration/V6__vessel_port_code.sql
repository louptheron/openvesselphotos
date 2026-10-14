-- UN/LOCODE of the port of registration (e.g. FRBES for Brest), from the EU fleet register: places the port on a map.
ALTER TABLE vessels ADD COLUMN port_code VARCHAR(5);

CREATE INDEX vessels_port_code_idx ON vessels (port_code);
