-- When the application of the API key a photo was sent with showed its review (approved or rejected) to the
-- contributor: it is then no longer returned by GET /v1/submissions.
ALTER TABLE photos ADD COLUMN seen_at TIMESTAMPTZ;
