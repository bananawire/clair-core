package com.claircore.device.infrastructure.persistence.jpa.entities;

import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Storage shape of {@code DeviceAssignment}.
 *
 * <p>{@code device_id} stays a plain unique column with the same name the {@code @ManyToOne} join
 * produced, so the schema is unchanged; what is gone is the object association behind it.
 */
@Entity
@Table(name = "device_assignments")
public class DeviceAssignmentPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "device_id", nullable = false, unique = true)
    private UUID deviceId;

    @Column(name = "owner_user_id")
    private UserId ownerUserId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @ElementCollection
    @CollectionTable(name = "device_assignment_configuration", joinColumns = @JoinColumn(name = "assignment_id"))
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value")
    private Map<String, String> configuration = new HashMap<>();

    @Column(name = "claim_token", unique = true)
    private ClaimToken claimToken;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    @Column(name = "presence_at")
    private Instant presenceAt;

    public DeviceAssignmentPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public UserId getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UserId ownerUserId) { this.ownerUserId = ownerUserId; }

    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }

    public Map<String, String> getConfiguration() { return configuration; }

    /** Replaces the contents in place; the collection instance is the one JPA is tracking. */
    public void setConfiguration(Map<String, String> configuration) {
        this.configuration.clear();
        if (configuration != null) {
            this.configuration.putAll(configuration);
        }
    }

    public ClaimToken getClaimToken() { return claimToken; }
    public void setClaimToken(ClaimToken claimToken) { this.claimToken = claimToken; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getPresenceAt() { return presenceAt; }
    public void setPresenceAt(Instant presenceAt) { this.presenceAt = presenceAt; }
}
