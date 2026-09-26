-- Owned trail data imported from OpenStreetMap. `id` is assigned by the import job
-- (not a DB sequence) so re-imports can upsert deterministically by osm_id.
CREATE TABLE trails (
    id         BIGINT PRIMARY KEY,
    osm_id     BIGINT UNIQUE,
    name       TEXT,
    geom       geometry(LineString, 4326) NOT NULL,
    length_m   DOUBLE PRECISION,
    difficulty TEXT,
    type       TEXT,
    tags       JSONB
);

-- GiST index powers ST_DWithin / nearest-neighbour "trails near here" queries.
CREATE INDEX idx_trails_geom ON trails USING GIST (geom);

-- Type filter is a common secondary predicate.
CREATE INDEX idx_trails_type ON trails (type);
