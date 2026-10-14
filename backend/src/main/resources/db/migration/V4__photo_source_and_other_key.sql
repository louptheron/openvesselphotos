-- Page of the original photo, for photos imported from Wikimedia Commons or Flickr: credited with the photo, and
-- unique so that an interrupted import can be run again without duplicating photos.
ALTER TABLE photos ADD COLUMN source_url VARCHAR(500);
CREATE UNIQUE INDEX photos_source_url_idx ON photos (source_url) WHERE source_url IS NOT NULL;

-- A contributor may know both keys of a vessel: the one of the other type than submitted_key_type.
ALTER TABLE photos ADD COLUMN submitted_other_key VARCHAR(12);
