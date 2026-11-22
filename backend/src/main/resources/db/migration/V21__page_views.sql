-- Daily page views of each website, counted without any personal data (no IP address, no cookie): the pages send their
-- path when shown, client-side navigations included.
-- site: OPEN_VESSEL_PHOTOS or LA_PECHE_FRANCAISE. path: the page path without query, or "other" beyond the paths
-- counted at once.
CREATE TABLE page_views (
    day   DATE         NOT NULL,
    site  VARCHAR(32)  NOT NULL,
    path  VARCHAR(255) NOT NULL,
    count BIGINT       NOT NULL,
    PRIMARY KEY (day, site, path)
);

-- Since this migration, usage_counts.source is "website" for the pages of Open Vessel Photos only, and
-- "ports-website" for those of La Pêche française: older "website" counts mix both sites.
COMMENT ON COLUMN usage_counts.source IS
    '"website" (Open Vessel Photos pages), "ports-website" (La Pêche française pages), "direct", "other" or the host of another site';
