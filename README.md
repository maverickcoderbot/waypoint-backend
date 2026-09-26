# Waypoint Backend

The backend for [Waypoint](https://github.com/maverickcoderbot/waypoint) — a free,
open-source "AllTrails-lite" trail finder. This service sits between the Waypoint PWA
and the outside world so the frontend stops calling public APIs directly: it owns
trail data, caching, and (soon) geocoding, tiles, and rate limiting.

**Stack:** Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL + **PostGIS** ·
Hibernate Spatial · Flyway.

## Architecture

Classic layered structure — **controller → service → repository → model** — organized
by feature:

```
ai.waypoint.backend
├── trail/         # the trail search feature (full slice)
│   ├── TrailController   (HTTP)   GET /api/trails
│   ├── TrailService      (logic)  spatial lookup + DTO mapping
│   ├── TrailRepository   (data)   PostGIS ST_DWithin query
│   ├── Trail             (model)  -> trails table
│   └── TrailDto          (API shape, GeoJSON-friendly coordinates)
├── cache/         # Postgres-backed cache for upstream responses
│   ├── CacheService · ApiCacheRepository · ApiCache  -> api_cache table
├── requestlog/    # request accounting for rate limiting / analytics
│   ├── RequestLogRepository · RequestLog             -> request_log table
└── config/
    └── WebConfig  (CORS for the PWA origins)
```

Schema is owned by **Flyway** (`src/main/resources/db/migration`), not Hibernate
(`ddl-auto=none`).

## The data workflow

Two-tier: *owned* data for imported regions, plus a *live-cache frontier* everywhere
else.

1. **Ingestion (batch):** OSM extract (Geofabrik `.osm.pbf`) → filter to trails
   (osmium/osm2pgsql) → upsert into the `trails` PostGIS table, keyed on `osm_id`.
2. **Serving:** `GET /api/trails?lat&lng&radius&type` → `TrailService` →
   `ST_DWithin` spatial query → GeoJSON coordinates, nearest first.
3. **Frontier fallback (planned):** on a miss, call live Overpass, return it, cache
   in `api_cache`, and queue that region for import.
4. **Ancillary data (planned):** geocoding / elevation / photos proxied through this
   service and cached in `api_cache`.

## Running locally

The `trails` feature needs a **PostGIS-capable Postgres** (plain Postgres won't do —
it lacks the `postgis` extension). Easiest is Docker:

```bash
docker run --name waypoint-pg -e POSTGRES_USER=waypoint \
  -e POSTGRES_PASSWORD=waypoint -e POSTGRES_DB=waypoint \
  -p 5432:5432 -d postgis/postgis:16-3.4
```

Then set the env (see `.env.example`) and run:

```bash
export DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/waypoint
export DATABASE_USER=waypoint DATABASE_PASSWORD=waypoint
./gradlew bootRun
```

Health check: `GET http://localhost:8080/actuator/health`.

## Build & test

```bash
./gradlew build   # compile + unit tests (no database required)
```

Unit tests are pure JUnit/Mockito and don't touch a DB, so CI stays simple.

## Roadmap

- **Phase 1:** adopt the caching Overpass proxy, wire `api_cache`. *(this scaffold)*
- **Phase 2:** geocoding + elevation proxies; self-host Protomaps tiles.
- **Phase 3:** OSM import job → serve `/api/trails` from owned PostGIS data.
- **Deploy:** Fly.io + Fly Postgres (target).
