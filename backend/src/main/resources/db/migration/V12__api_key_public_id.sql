-- Public, non-secret identifier of an approved API key's application: a link to the contribute page carries it
-- (?app=), so that photos sent through the website by that application's users are tied to the key, like those sent
-- with it, and can be followed with GET /v1/submissions?contributorId=.
ALTER TABLE api_keys ADD COLUMN public_id VARCHAR(16) UNIQUE;
UPDATE api_keys SET public_id = 'app_' || substr(md5(random()::text || id::text), 1, 12) WHERE key_hash IS NOT NULL;
