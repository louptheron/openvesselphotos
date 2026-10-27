-- Pseudonymous identifier of the contributor in the application of the API key the photo was sent with (e.g. a HMAC
-- of its user id, computed by that application with a key it keeps): groups a contributor's photos without knowing
-- who they are. Never public.
ALTER TABLE photos ADD COLUMN contributor_id VARCHAR(64);
CREATE INDEX photos_contributor_idx ON photos (api_key_id, contributor_id) WHERE contributor_id IS NOT NULL;
