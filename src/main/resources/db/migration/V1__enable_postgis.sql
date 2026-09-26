-- PostGIS provides the geometry type, spatial indexes, and ST_* functions used by
-- the trails table. Requires a PostGIS-capable Postgres (e.g. the postgis/postgis
-- image, or a managed Postgres with the extension available).
CREATE EXTENSION IF NOT EXISTS postgis;
