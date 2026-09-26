package ai.waypoint.backend.trail;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Trail}.
 *
 * <p>The main read is {@link #findNear}: a PostGIS spatial query that returns trails
 * within {@code radiusMeters} of a point, nearest first. It casts to {@code geography}
 * so the radius and ordering are true metres, and uses {@code ST_DWithin} so the GiST
 * index on {@code geom} is used. An optional {@code type} filter narrows by mode.
 */
public interface TrailRepository extends JpaRepository<Trail, Long> {

    @Query(value = """
            SELECT * FROM trails t
            WHERE ST_DWithin(
                    t.geom::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters)
              AND (:type IS NULL OR t.type = :type)
            ORDER BY t.geom::geography <->
                     ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            LIMIT :limit
            """, nativeQuery = true)
    List<Trail> findNear(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("type") String type,
            @Param("limit") int limit);

    boolean existsByOsmId(Long osmId);
}
