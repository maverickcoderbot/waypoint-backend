package ai.waypoint.backend.overpass;

import static org.assertj.core.api.Assertions.assertThat;

import ai.waypoint.backend.geo.Bbox;
import org.junit.jupiter.api.Test;

class GeoFenceTest {

    private final Bbox bbox = new Bbox(38.35, -90.9, 39.05, -89.95);

    @Test
    void acceptsAroundCenterInRegion() {
        assertThat(GeoFence.regionViolation("node(around:1000,38.6,-90.3);out;", bbox)).isEmpty();
    }

    @Test
    void acceptsBoundingBoxIncludingBoundaryCoordinates() {
        assertThat(GeoFence.regionViolation("way(38.35,-90.9,39.05,-89.95);out;", bbox)).isEmpty();
    }

    @Test
    void acceptsWhitespaceInBothForms() {
        assertThat(GeoFence.regionViolation(
                "node(around: 100, 38.6, -90.3);way( 38.5, -90.5, 38.7, -90.2 );", bbox)).isEmpty();
    }

    @Test
    void rejectsAnyOutOfRegionCenter() {
        assertThat(GeoFence.regionViolation(
                "node(around:100,38.6,-90.3);node(around:100,40,-90.3);", bbox))
                .contains("Around coordinate is outside the configured region");
    }

    @Test
    void checksBothBoundingBoxCorners() {
        assertThat(GeoFence.regionViolation("way(38.2,-90.5,38.7,-90.2);", bbox)).isPresent();
        assertThat(GeoFence.regionViolation("way(38.5,-90.5,39.2,-90.2);", bbox)).isPresent();
        assertThat(GeoFence.regionViolation("way(38.5,-91,38.7,-90.2);", bbox)).isPresent();
        assertThat(GeoFence.regionViolation("way(38.5,-90.5,38.7,-89);", bbox)).isPresent();
    }

    @Test
    void checksBoxEvenWhenAroundCenterIsValid() {
        assertThat(GeoFence.regionViolation(
                "node(around:100,38.6,-90.3);way(38.5,-90.5,40,-90.2);", bbox)).isPresent();
    }

    @Test
    void rejectsQueriesWithoutCoordinates() {
        assertThat(GeoFence.regionViolation("node[highway];out;", bbox))
                .contains("Query has no geofenceable coordinates");
        assertThat(GeoFence.regionViolation("", bbox)).isPresent();
    }
}
