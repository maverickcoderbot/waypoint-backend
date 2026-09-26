package ai.waypoint.backend.requestlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One inbound API request, recorded for rate limiting and (later) analytics.
 *
 * <p>Maps to the {@code request_log} table. Kept deliberately small; a periodic
 * job trims rows older than the retention window.
 */
@Entity
@Table(name = "request_log")
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Client identifier (hashed IP), never a raw address. */
    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "status")
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RequestLog() {
        // for JPA
    }

    public RequestLog(String clientId, String path, Integer status, Instant createdAt) {
        this.clientId = clientId;
        this.path = path;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
