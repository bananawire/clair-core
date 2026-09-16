package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.model.valueobjects.ProvisionedDevice;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.assemblers.DevicePersistenceAssembler;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DevicePersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

    /** Lower than any stored timestamp or id, so the first page is unfiltered. See the query. */
    private static final Instant CURSOR_START = Instant.EPOCH;
    private static final UUID ID_CURSOR_START = new UUID(0L, 0L);

    private final DevicePersistenceRepository devicePersistenceRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final String ROSTER_FROM = """
            FROM devices d LEFT JOIN device_assignments a ON a.device_id = d.id
            WHERE greatest(d.updated_at, coalesce(a.updated_at, d.updated_at)) > ?
               OR (greatest(d.updated_at, coalesce(a.updated_at, d.updated_at)) = ? AND d.id > ?)
            """;


    public DeviceRepositoryImpl(DevicePersistenceRepository devicePersistenceRepository, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.devicePersistenceRepository = devicePersistenceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Device save(Device device) {
        var saved = devicePersistenceRepository.save(DevicePersistenceAssembler.toPersistenceFromDomain(device));
        return DevicePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public void advanceRosterWatermark(UUID deviceId, Instant previousWatermark) {
        Instant watermark = Instant.now();
        if (previousWatermark != null && !watermark.isAfter(previousWatermark.plusNanos(1000))) {
            watermark = previousWatermark.plusNanos(1000);
        }
        devicePersistenceRepository.advanceRosterWatermark(deviceId, watermark);
    }

    @Override
    public List<Device> saveAll(Collection<Device> devices) {
        var entities = devices.stream().map(DevicePersistenceAssembler::toPersistenceFromDomain).toList();
        return devicePersistenceRepository.saveAll(entities).stream()
                .map(DevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Device> findById(UUID id) {
        return devicePersistenceRepository.findById(id).map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Device> findAllById(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return devicePersistenceRepository.findAllById(ids).stream()
                .map(DevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Device> findBySerialNumber(String serialNumber) {
        return devicePersistenceRepository.findBySerialNumber(serialNumber)
                .map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Device> findAllBySerialNumberIn(Collection<String> serialNumbers) {
        if (serialNumbers == null || serialNumbers.isEmpty()) return List.of();
        return devicePersistenceRepository.findAllBySerialNumberIn(serialNumbers).stream()
                .map(DevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Device> findByHardwareId(String hardwareId) {
        return devicePersistenceRepository.findByHardwareId(new HardwareId(hardwareId))
                .map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Device> findByHardwareIdForUpdate(String hardwareId) {
        return devicePersistenceRepository.findByHardwareIdForUpdate(new HardwareId(hardwareId))
                .map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Device> findByApiKey(String apiKey) {
        return devicePersistenceRepository.findByApiKey(new ApiKey(apiKey))
                .map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByHardwareId(String hardwareId) {
        return devicePersistenceRepository.existsByHardwareId(new HardwareId(hardwareId));
    }

    @Override
    public PageResult<Device> findDevicesForTelemetry(int limit, boolean includeDeleted) {
        int capped = Math.max(1, Math.min(limit, 500));
        var pageable = org.springframework.data.domain.PageRequest.of(0, capped);
        var rows = includeDeleted
                ? devicePersistenceRepository.findAllByOrderByIdAsc(pageable)
                : devicePersistenceRepository.findAllByDeletedFalseOrderByIdAsc(pageable);
        var devices = rows.getContent().stream()
                .map(DevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
        return new PageResult<>(devices, 0, capped, devices.size());
    }

    @Override
    public PageResult<ProvisionedDevice> findProvisionedDevices(Instant since, UUID afterId, int limit) {
        devicePersistenceRepository.flush();
        // Read JDBC values explicitly: interface projections stringify VOs and can reinterpret
        // CASE timestamp results in the JVM timezone instead of preserving the stored instant.
        // The cursor is rounded to microseconds because the column is `timestamp(6) with time
        // zone`: an in-memory Instant can carry nanoseconds (e.g. 924869770Z) that H2 rounds up
        // to 924870Z on disk. If we truncated instead, the cursor would lag behind the stored
        // value and let a row at the boundary slip past the `> cursor` half of the WHERE clause
        // and reappear on the next page.
        var cursor = (since != null ? since : CURSOR_START)
                .plusNanos(500L)
                .truncatedTo(ChronoUnit.MICROS)
                .atOffset(java.time.ZoneOffset.UTC);
        var id = afterId != null ? afterId : ID_CURSOR_START;
        var rows = jdbcTemplate.query("""
                SELECT d.id, a.id AS assignment_id, d.hardware_id, d.api_key, coalesce(a.status, 'OFFLINE') AS status,
                       coalesce(d.deleted, false) AS deleted,
                       greatest(d.updated_at, coalesce(a.updated_at, d.updated_at)) AS updated_at
                """ + ROSTER_FROM + " ORDER BY updated_at ASC, d.id ASC LIMIT ?",
                (rs, rowNum) -> new ProvisionedDevice(
                        rs.getObject("id", UUID.class), rs.getObject("assignment_id", UUID.class),
                        rs.getString("hardware_id"), rs.getString("api_key"),
                        com.claircore.device.domain.model.valueobjects.DeviceStatus.valueOf(rs.getString("status")),
                        rs.getBoolean("deleted"), rs.getTimestamp("updated_at").toInstant()),
                cursor, cursor, id, limit);
        Long total = jdbcTemplate.queryForObject("SELECT count(*) " + ROSTER_FROM, Long.class, cursor, cursor, id);
        return new PageResult<>(rows, 0, limit, total == null ? 0 : total);
    }
}
