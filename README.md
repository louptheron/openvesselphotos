# Open Vessel Photos

A free, open-source library of vessel photos, searchable by **CFR** (EU fleet register) or **MMSI** (radio identity).

- a public site: search, vessel pages, photo contribution;
- a free JSON + image API, with no key: `/v1/cfr/FRA000123456.jpg`, `/v1/mmsi/227000101`, … (see `/docs`);
- a review queue (`/admin`) protected by a unique admin password.

Every photo is published only after review. Uploads are re-encoded as JPEG in 3 sizes (320 px, 800 px, full ≤ 2400
px), which also strips EXIF/GPS metadata.

## Stack

Same conventions as [MonitorFish](https://github.com/MTES-MCT/monitorfish):

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

Like in MonitorFish, `make run-back` reads its variables from `backend/.env.local.defaults` (admin password: `admin`),
overridden by an optional `.env` file at the root. The database uses port 5433, so that it runs alongside MonitorFish's
(5432).

```sh
make test            # backend (ktlint + tests, needs Docker for Testcontainers) and frontend checks
make lint-back       # format backend code
make lint-front      # fix and format frontend code
make run-app-docker  # production image + database, on http://localhost:8880
```

## Configuration

Variables are listed in `backend/.env.example`.

| Variable                                          | Default                    | Description                                         |
| ------------------------------------------------- | -------------------------- | --------------------------------------------------- |
| `SCALINGO_POSTGRESQL_URL`, `DATABASE_URL`         | _(set by Scalingo)_        | Database URL, take precedence over the next one     |
| `OPENVESSELPHOTOS_DATABASE_URL`                   | _(required otherwise)_     | `postgresql://user:password@host:port/db` or `jdbc:` |
| `OPENVESSELPHOTOS_DATABASE_HIKARI_MAX_POOL_SIZE`  | `5`                        | Database connections per container                  |
| `OPENVESSELPHOTOS_ADMIN_PASSWORD`                 | _(empty: admin disabled)_  | Unique password of `/admin`                         |
| `PORT`                                            | _(set by Scalingo)_        | HTTP port, takes precedence over the next one       |
| `OPENVESSELPHOTOS_SERVER_PORT`                    | `8880`                     | HTTP port                                           |
| `JAVA_OPTS`                                       |                            | JVM options, e.g. `-XX:MaxRAMPercentage=75`         |

The admin session is a signed, HttpOnly cookie valid 12 hours. Its key derives from the password: changing
`OPENVESSELPHOTOS_ADMIN_PASSWORD` signs everybody out. After 10 wrong passwords, an IP is locked out for 15 minutes.

### Differences with MonitorFish

- The Gradle wrapper is at the root (not in `backend/`): Scalingo's Gradle buildpack requires it there.
- The database URL also comes from `SCALINGO_POSTGRESQL_URL` / `DATABASE_URL`, in Scalingo's `postgresql://` format.
- Plain PostgreSQL (no PostGIS/TimescaleDB image), default Spring Boot logging (no Log4j2), no Keycloak.
- Frontend: Vitest instead of Jest, no `import-meta-env` (no runtime frontend variable), native oxlint rules only.

## Deployment on Scalingo

Scalingo builds with buildpacks, not Dockerfiles. `.buildpacks` chains the Node.js buildpack (the root `package.json`
builds the frontend) and the Gradle buildpack (`./gradlew stage` packages it in the jar). `Procfile` starts the jar,
`system.properties` selects Java 21.

```sh
scalingo create openvesselphotos --region osc-fr1
scalingo --app openvesselphotos addons-add postgresql postgresql-starter-512
scalingo --app openvesselphotos env-set OPENVESSELPHOTOS_ADMIN_PASSWORD="$(openssl rand -base64 24)"
scalingo --app openvesselphotos env-set JAVA_OPTS="-XX:MaxRAMPercentage=75"
git push scalingo main
```

Use a container of at least size M (512 MB): Spring Boot and image resizing need the memory. Flyway migrates the
database at startup. The Docker image (`infra/docker/app/Dockerfile`) is for local use and self-hosting.

Keep an eye on the database size: each photo takes about 1 MB (three JPEG sizes).

## API

See `/docs` on a running instance. In short:

| Method | Path                                         | Description                                   |
| ------ | -------------------------------------------- | --------------------------------------------- |
| GET    | `/v1/{cfr\|mmsi}/{key}`                       | Vessel, keys, published photos, history       |
| GET    | `/v1/{cfr\|mmsi}/{key}.jpg?size=320\|800\|full` | Main photo (default 800)                      |
| GET    | `/v1/{cfr\|mmsi}/{key}/photos/{n}.jpg`        | Photo `n`, same file under both keys          |
| GET    | `/v1/search?q=`                              | Search by name, CFR or MMSI prefix            |
| GET    | `/v1/vessels/recent`                         | Latest published vessels                      |
| GET    | `/v1/export.json`                            | Whole dataset                                 |
| POST   | `/v1/{cfr\|mmsi}/{key}/photos`                | Contribute a photo (multipart)                |
| POST   | `/v1/{cfr\|mmsi}/{key}/link`                  | Suggest the missing key                       |
| POST   | `/v1/{cfr\|mmsi}/{key}/photos/{n}/reports`    | Report a photo                                |
