package ai.waypoint.backend.trail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrailServiceTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private TrailRepository trailRepository;

    @InjectMocks
    private TrailService trailService;

    @Test
    void mapsGeometryToLngLatCoordinateOrder() {
        Trail trail = new Trail();
        trail.setId(1L);
        trail.setName("Creek Loop");
        trail.setType("walk");
        trail.setGeom(line(-90.30, 38.60, -90.31, 38.61));
        when(trailRepository.findNear(anyDouble(), anyDouble(), anyDouble(), eq("walk"), eq(150)))
                .thenReturn(List.of(trail));

        List<TrailDto> result = trailService.findNear(38.60, -90.30, 7000, "walk", 150);

        assertThat(result).hasSize(1);
        TrailDto dto = result.get(0);
        assertThat(dto.name()).isEqualTo("Creek Loop");
        // GeoJSON order: [lng, lat]
        assertThat(dto.coordinates().get(0)).containsExactly(-90.30, 38.60);
        assertThat(dto.coordinates().get(1)).containsExactly(-90.31, 38.61);
    }

    @Test
    void clampsLimitToMaxAndNormalizesBlankType() {
        when(trailRepository.findNear(anyDouble(), anyDouble(), anyDouble(), eq(null), eq(150)))
                .thenReturn(List.of());

        trailService.findNear(38.60, -90.30, 7000, "   ", 10_000);

        // limit clamped from 10_000 to 150; blank type normalized to null
        verify(trailRepository).findNear(38.60, -90.30, 7000, null, 150);
    }

    private static LineString line(double... lngLat) {
        Coordinate[] coords = new Coordinate[lngLat.length / 2];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = new Coordinate(lngLat[i * 2], lngLat[i * 2 + 1]);
        }
        return GF.createLineString(coords);
    }
}
