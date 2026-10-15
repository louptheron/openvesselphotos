-- Day of the last import of the EU fleet register that gave the vessel its technical data.
ALTER TABLE vessels ADD COLUMN register_imported_on DATE;
