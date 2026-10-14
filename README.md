# Open Vessel Photos

[![CI](https://github.com/louptheron/openvesselphotos/actions/workflows/ci.yml/badge.svg)](https://github.com/louptheron/openvesselphotos/actions/workflows/ci.yml)

A free, open-source library of vessel photos, searchable by **CFR** (EU fleet register) or **MMSI** (radio identity).

- a public site: search, vessel pages, photo contribution;
- a free JSON + image API, with no key: `/v1/cfr/FRA000123456.jpg`, `/v1/mmsi/227000101`, … (see `/docs`);
- a review queue (`/admin`) protected by a unique admin password.

Every photo is published only after review. Uploads are re-encoded as JPEG in 3 sizes (320 px, 800 px, full ≤ 1600
px), which also strips EXIF/GPS metadata.

## Stack

| Part     | Technology                                                                       |
| -------- | -------------------------------------------------------------------------------- |
| Backend  | Kotlin, Spring Boot 4, JPA, Flyway, hexagonal layout (`domain` / `infrastructure`) |
| Frontend | React 18, TypeScript, Vite, Redux Toolkit Query, styled-components                |
| Database | PostgreSQL (vessels, photos **and** image files, in `bytea`)                      |
| Tests    | JUnit + Testcontainers, Vitest                                                    |
| Lint     | ktlint, oxlint, Prettier, `tsc`                                                   |

The jar serves both the API (`/v1/**`) and the built React app: one process, one database, nothing else to host.
Images live in PostgreSQL so that the containers stay stateless.

```
backend/src/main/kotlin/org/openvesselphotos/
├── domain/            entities, use cases (one class, one `execute`), repository interfaces
├── infrastructure/    REST controllers, JPA repositories, image processing
└── config/            database URL, admin authentication, CORS, React routes
frontend/src/
├── api/               RTK Query endpoints and types
├── components/        layout, shared UI
└── features/          Home, Vessel, Contribute, Docs, Admin
```

## Development

Requirements: JDK 21, Node 24, Docker. Run `make` to list all commands.

```sh
make install-front   # frontend dependencies
make run-back        # PostgreSQL (Docker, port 5433) + API on http://localhost:8880
make run-front       # React app on http://localhost:3000 (proxies /v1 to the API)
```

`make run-back` reads its variables from `backend/.env.local.defaults` (admin password: `admin`),
overridden by an optional `.env` file at the root. The database uses port 5433.

```sh
make test            # backend (ktlint + tests, needs Docker for Testcontainers) and frontend checks
make lint-back       # format backend code
make lint-front      # fix and format frontend code
make run-app-docker  # production image + database, on http://localhost:8880
```

## CI

GitHub Actions, on every pull request and on `main`:

- `ci.yml`: backend lint and tests (`make test-back`), frontend lint and tests (`make test-front`), the build as on
  Scalingo (`npm run scalingo-postbuild` then `./gradlew stage`) and the Docker image, in parallel.
- `security.yml`: Trivy scans the Docker image (OS packages and Java dependencies) on `main` and every week; results
  are in the repository's Security tab.
- Dependabot opens monthly grouped updates for GitHub Actions, Gradle, npm and the Docker base images.

## Configuration

Variables are listed in `backend/.env.example`.

| Variable                                          | Default                    | Description                                         |
| ------------------------------------------------- | -------------------------- | --------------------------------------------------- |
| `SCALINGO_POSTGRESQL_URL`, `DATABASE_URL`         | _(set by Scalingo)_        | Database URL, take precedence over the next one     |
| `OPENVESSELPHOTOS_DATABASE_URL`                   | _(required otherwise)_     | `postgresql://user:password@host:port/db` or `jdbc:` |
| `OPENVESSELPHOTOS_DATABASE_HIKARI_MAX_POOL_SIZE`  | `5`                        | Database connections per container                  |
| `OPENVESSELPHOTOS_PUBLIC_URL`                     | _(requested host)_         | Canonical base URL, e.g. `https://example.org`      |
| `OPENVESSELPHOTOS_ADMIN_PASSWORD`                 | _(empty: admin disabled)_  | Unique password of `/admin`                         |
| `PORT`                                            | _(set by Scalingo)_        | HTTP port, takes precedence over the next one       |
| `OPENVESSELPHOTOS_SERVER_PORT`                    | `8880`                     | HTTP port                                           |
| `JAVA_OPTS`                                       |                            | Extra JVM options, override those of the `Procfile` |
| `VITE_CARTO_BASEMAP_KEY`                          | _(OpenStreetMap tiles)_    | CARTO basemap key of the ports map, read at build   |

The admin session is a signed, HttpOnly cookie valid 12 hours. Its key derives from the password: changing
`OPENVESSELPHOTOS_ADMIN_PASSWORD` signs everybody out. After 10 wrong passwords, an IP is locked out for 15 minutes.

### Vessels without photos

Vessels can exist before their first photo: their page then invites visitors to add one, and search engines index it
(`/sitemap.xml` is an index of `sitemap-vessels-N.xml` files of 10,000 vessels). Import them from the "Import vessels"
section of `/admin`:

- an export of the [EU fleet register](https://webgate.ec.europa.eu/fleet-europa/search_en) (CSV, current fleet of a
  country; columns `CFR`, `MMSI`, `Name of Vessel`, `External Marking`, `Place of Registration Name`,
  `Place of Registration Code`, or `Vessel Name`, `Ext. Marking`, `Port Name` in older exports), or
- a CSV with the columns `cfr`, `mmsi`, `name`, `registration` (hull marking, e.g. `LO 752221`), `port`, `port_code`.

### Ports

`/ports` shows the ports of registration on a map, each as a circle sized by its number of vessels (written inside),
coloured and ringed by the share of them with a photo, then lists them all in a table (search, sort by column,
pagination; the state is in the URL). `/ports/{code}` lists a port's vessels, photographed or still waiting for a photo.
For now only France's register is imported, which the page says.

Ports are identified by their UN/LOCODE (the register's `Place of Registration Code`, e.g. `FRBES` for Brest), placed with
the coordinates of `backend/src/main/resources/ports.csv`, built from UN/LOCODE by `backend/scripts/ports.py`.

The map uses [Leaflet](https://leafletjs.com), loaded only where a map is shown, and CARTO's light basemap (Positron,
from OpenStreetMap data), which needs a key: set `VITE_CARTO_BASEMAP_KEY` when building the frontend (on Scalingo, as
an environment variable: the build reads it). Without it, for instance in development, the map shows OpenStreetMap's
standard tiles. The key ends up in the tile URLs, so restrict it to the site's domain in CARTO's dashboard. Visitors'
browsers load the tiles from CARTO's servers.

### Imported photos

Openly licensed photos found elsewhere (Wikimedia Commons, Flickr) can be published without review from the "Import
photos" section of `/admin`, with a CSV of the columns `mmsi` and/or `cfr`, `vessel_name`, `author`, `licence` (SPDX
identifier or label, e.g. `CC-BY-SA-3.0` or `CC BY-SA 3.0`), `file_page` (the page of the original photo, linked from
its credit), `image_1600_url` and, optionally, `date`. The images are downloaded from `wikimedia.org` and
`staticflickr.com` only, one at a time and in the background (under a second per photo); `/admin` shows the progress.
Photos are recognized by their `file_page`, so a file can be sent again after an interruption without duplicates.

### Statistics

`/admin/stats` shows how the photos and the API are used over the last 7 days to a year: photo views by the site that
shows them (the host of the `Referer` header sent with an `<img>`), API calls by API key (a key sent with a GET request
is optional, but identifies the caller), and the API keys used. Counts are aggregated per day, without IP addresses or
any other personal data, kept in memory and saved every minute. Photos are cached for a day by browsers, so a visitor
seeing the same photo again that day is not counted twice.

### API keys

`GET` requests are open. `POST` requests to the public API need an API key in the `x-api-key` header
(`ApiKeyCheckFilter`), except those from the website's own forms: browsers mark them as same-origin
(`Sec-Fetch-Site`), and a key cannot be hidden in frontend code.

Anyone can request a key at `/api-key`. Requests appear in the "API keys" tab of `/admin`: approving one creates the key
and shows it once, with a ready-made email to send it (there is no email sending in the app). Only its SHA-256 hash is
stored. Keys can be revoked there, and photos sent with a key show its name in the review queue.

### Rate limits

In-memory counters per container (`RateLimitFilter`), adjustable with environment variables:

| Requests                                   | Default limit          | Variable                                               |
| ------------------------------------------ | ---------------------- | ------------------------------------------------------ |
| `GET /v1/**`                               | 600 per minute per IP  | `OPENVESSELPHOTOS_RATE_LIMIT_READS_PER_MINUTE`          |
| `POST /v1/**` without key (website forms)  | 20 per hour per IP     | `OPENVESSELPHOTOS_RATE_LIMIT_WRITES_PER_HOUR`           |
| `POST /v1/**` with an API key              | 1,000 per hour per key | `OPENVESSELPHOTOS_RATE_LIMIT_API_KEY_WRITES_PER_HOUR`   |
| `POST /v1/api-key-requests`                | 5 per hour per IP      | `OPENVESSELPHOTOS_RATE_LIMIT_API_KEY_REQUESTS_PER_HOUR` |

Above a limit, the API returns a `429` with `Retry-After`. With several containers, each counts separately.

The client IP is the TCP address, unless `OPENVESSELPHOTOS_CLIENT_IP_HEADER` names a header set by the proxy in front of
the app: on Scalingo, `X-Real-IP`. Never set it without such a proxy, or clients could choose their IP.

Behind Cloudflare's proxy (orange cloud), that address is a Cloudflare server: also set
`OPENVESSELPHOTOS_BEHIND_CLOUDFLARE=true`. The client IP is then the `CF-Connecting-IP` header, trusted only when the
request comes from one of [Cloudflare's addresses](https://www.cloudflare.com/ips/) (listed in `ClientIp`). A request
sent to the app's `*.scalingo.io` domain directly keeps its own address, so that header can't be used to dodge the rate
limits.

### Search engines

The backend serves the React app's `index.html` with, for each page, its title, description, canonical URL, Open Graph
tags, schema.org data (`ImageObject` with licence and credit for each photo) and a server-rendered copy of its content,
which React replaces once loaded (`PagesController`). Unknown vessels and pages return a real 404, search results are
`noindex`. `/sitemap.xml` lists every vessel page and its images, `/robots.txt` points to it.

With a custom domain, set `OPENVESSELPHOTOS_PUBLIC_URL` so that canonical links never point to `*.scalingo.io`, and
submit `https://<domain>/sitemap.xml` in Google Search Console.

## Deployment on Scalingo

Scalingo builds with buildpacks, not Dockerfiles. `.buildpacks` chains the Node.js buildpack (the root `package.json`
builds the frontend) and the Gradle buildpack (`./gradlew stage` packages it in the jar). `Procfile` starts the jar,
`system.properties` selects Java 21.

```sh
scalingo create openvesselphotos --region osc-fr1
scalingo --app openvesselphotos scale web:1:S
scalingo --app openvesselphotos addons-add postgresql postgresql-starter-512
scalingo --app openvesselphotos env-set OPENVESSELPHOTOS_ADMIN_PASSWORD="$(openssl rand -base64 24)"
scalingo --app openvesselphotos env-set OPENVESSELPHOTOS_CLIENT_IP_HEADER=X-Real-IP
git push scalingo main
```

Flyway migrates the database at startup. The Docker image (`infra/docker/app/Dockerfile`) is for local use and self-hosting.

## Costs

The cheapest setup on Scalingo is one **S** container and a **Starter 512** database: €14.40 excl. VAT per 30 days
(€7.20 each, prices of October 2026). Containers can't scale to zero and there is no free database plan.

The app is sized for it:

- **256 MB of memory.** The `Procfile` (and the Dockerfile) set the JVM options: 96 MB heap, serial GC, small code
  cache. Tested under S limits (256 MB, 64 MB swap, 128 processes, ¼ CPU) with 3 simultaneous uploads of 24-megapixel
  photos: no error, at most 49 MB of swap. Without these options, the app is killed for lack of memory. Startup takes
  about 40 s on a quarter of a CPU.
- **Uploads are decoded at reduced resolution** (source subsampling) and **one at a time**, so a large photo never sits
  in memory at full size. Images over 100 megapixels are refused.
- **20 Tomcat threads and 3 database connections**: an S container allows 128 threads, Starter 512 allows 30
  connections.
- **Storage**: images are kept in PostgreSQL. Full size is capped at 1600 px (JPEG quality 82). Starter 512 includes
  10 GB, enough for roughly 20,000 photos (about 0.5 MB each, all sizes). Beyond, storage costs about €0.50 per GB
  per month. Rejected and removed photos are deleted.
- **Cache**: images are cached for a day by browsers and proxies, which spares the small container. A free CDN in
  front of the domain (e.g. Cloudflare) would serve most image requests without reaching it.

If the app gets slow or swaps a lot, move to M (€14.40) with `scalingo scale web:1:M` and
`JAVA_OPTS="-Xmx256m -XX:MaxMetaspaceSize=160m"`.

## API

See `/docs` on a running instance. In short:

| Method | Path                                         | Description                                   |
| ------ | -------------------------------------------- | --------------------------------------------- |
| GET    | `/v1/{cfr\|mmsi}/{key}`                       | Vessel, keys, published photos, history       |
| GET    | `/v1/{cfr\|mmsi}/{key}.jpg?size=320\|800\|full` | Main photo (default 800)                      |
| GET    | `/v1/{cfr\|mmsi}/{key}/photos/{n}.jpg`        | Photo `n`, same file under both keys          |
| GET    | `/v1/search?q=`                              | Search by name, CFR or MMSI prefix            |
| GET    | `/v1/vessels/recent`                         | Latest published vessels                      |
| GET    | `/v1/coverage`                               | Vessels of the register photographed, counts  |
| GET    | `/v1/ports`, `/v1/ports/{code}`              | Ports, coordinates, vessels photographed      |
| GET    | `/v1/ports/{code}/vessels?photographed=`     | Vessels of a port, with or without a photo    |
| GET    | `/v1/export.json`                            | Whole dataset                                 |
| POST   | `/v1/{cfr\|mmsi}/{key}/photos`                | Contribute a photo (multipart), API key       |
| POST   | `/v1/{cfr\|mmsi}/{key}/link`                  | Suggest the missing key                       |
| POST   | `/v1/{cfr\|mmsi}/{key}/photos/{n}/reports`    | Report a photo                                |
| POST   | `/v1/api-key-requests`                       | Request an API key (no key needed)            |

## Licence

Open Vessel Photos — Copyright © 2026 Loup Theron and contributors.

The code is free software: you can redistribute it and/or modify it under the terms of the
[GNU Affero General Public License](LICENSE), version 3 or (at your option) any later version. Anyone may run their
own instance; a modified version offered as a service must publish its source code too.

The photos are not covered by this licence: each one keeps the open licence chosen by its author (CC BY 4.0,
CC BY-SA 4.0 or CC0; photos imported from Wikimedia Commons or Flickr can also be under CC BY or CC BY-SA 1.0 to 3.0,
or in the public domain), shown with it on the site and in the API.

