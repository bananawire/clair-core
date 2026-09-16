package com.claircore.device.domain.repositories;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.valueobjects.ProvisionedDevice;
import com.claircore.shared.domain.model.PageResult;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for device storage. Domain types only. */
public interface DeviceRepository {

    Device save(Device device);

    /** Advances the roster after an assignment is removed, even if no device field changed. */
    void advanceRosterWatermark(UUID deviceId, Instant previousWatermark);

    List<Device> saveAll(Collection<Device> devices);

    Optional<Device> findById(UUID id);

    List<Device> findAllById(Collection<UUID> ids);

    Optional<Device> findBySerialNumber(String serialNumber);

    List<Device> findAllBySerialNumberIn(Collection<String> serialNumbers);

    Optional<Device> findByHardwareId(String hardwareId);
    /**
     * Row-locks the inventory device for the transaction. A first pairing has no assignment row to
     * lock yet, so the device row is what serializes two simultaneous first pairs of one unit.
     */
    Optional<Device> findByHardwareIdForUpdate(String hardwareId);

    Optional<Device> findByApiKey(String apiKey);

    boolean existsByHardwareId(String hardwareId);

    /**
     * The edge roster, as a keyset page ordered by the later of the device's and its assignment's
     * {@code updatedAt}, then by id. A null {@code since} or {@code afterId} starts from the
     * beginning; the cursor only ever moves forward.
     */
    PageResult<ProvisionedDevice> findProvisionedDevices(Instant since, UUID afterId, int limit);

    /**
     * Page of devices ordered by id, used by LocalEdge to pick telemetry targets without
     * leaking the roster query shape.
     */
    PageResult<Device> findDevicesForTelemetry(int limit, boolean includeDeleted);
}
