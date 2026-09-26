package ai.waypoint.backend.trail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.LineString;

/**
 * A hiking/walking trail owned by Waypoint, imported from OpenStreetMap.
 *
 * <p>Maps to the {@code trails} PostGIS table. The {@link #geom} column is a
 * {@code geometry(LineString, 4326)} (WGS-84 lng/lat) and is indexed with a GiST
 * index so spatial "near here" queries stay fast. See
 * {@link TrailRepository#findNear} for the read path.
 */
@Entity
@Table(name = "trails")
public class Trail {

    @Id
    @Column(name = "id")
    private Long id;

    /** Source OSM element id (way/relation), used to keep imports idempotent. */
    @Column(name = "osm_id")
    private Long osmId;

    @Column(name = "name")
    private String name;

    /** Trail geometry in WGS-84 (SRID 4326). Coordinates are (lng, lat). */
    @Column(name = "geom", columnDefinition = "geometry(LineString,4326)")
    private LineString geom;

    /** Length in metres, computed at import time. */
    @Column(name = "length_m")
    private Double lengthM;

    /** Coarse difficulty heuristic: easy | moderate | hard. */
    @Column(name = "difficulty")
    private String difficulty;

    /** Mode this trail suits: walk | cycle | vehicle. */
    @Column(name = "type")
    private String type;

    /** Raw OSM tags, retained for later filtering/inspection. */
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    protected Trail() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOsmId() {
        return osmId;
    }

    public void setOsmId(Long osmId) {
        this.osmId = osmId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LineString getGeom() {
        return geom;
    }

    public void setGeom(LineString geom) {
        this.geom = geom;
    }

    public Double getLengthM() {
        return lengthM;
    }

    public void setLengthM(Double lengthM) {
        this.lengthM = lengthM;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
