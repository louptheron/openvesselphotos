-- Vessels can exist without photos (imported from the EU fleet register, for instance).
-- registration: the marking painted on the hull (e.g. "LO 752221"); port: the port of registration.
ALTER TABLE vessels ADD COLUMN registration VARCHAR(20);
ALTER TABLE vessels ADD COLUMN port VARCHAR(100);
