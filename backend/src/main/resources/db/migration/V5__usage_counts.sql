-- Daily usage of the public API and of the photos, counted without any personal data (no IP address): images by the
-- site that shows them (the host of the Referer header), API calls by API key.
-- source: "website" (this site), "direct" (no Referer: apps, scripts, privacy settings) or the host of the other site.
-- api_key_id: 0 when the request had no valid API key.
CREATE TABLE usage_counts (
    day        DATE         NOT NULL,
    kind       VARCHAR(8)   NOT NULL,
    source     VARCHAR(255) NOT NULL,
    api_key_id INTEGER      NOT NULL,
    count      BIGINT       NOT NULL,
    PRIMARY KEY (day, kind, source, api_key_id)
);
