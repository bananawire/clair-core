package com.claircore.device.domain.model.commands;

import java.util.List;

/**
 * Adds factory inventory from an explicit list. Rows whose serial number or hardware id already
 * exist are skipped, so the same file can be applied on every start.
 */
public record ImportDevicesCommand(List<DeviceProvisioningRecord> records) {
    public ImportDevicesCommand {
        if (records == null) {
            throw new IllegalArgumentException("Records must not be null");
        }
        records = List.copyOf(records);
    }

    /** One inventory row as the factory (or the demo seeder) supplies it. */
    public record DeviceProvisioningRecord(String serialNumber, String hardwareId, String apiKey, String name) {
        public DeviceProvisioningRecord {
            if (serialNumber == null || serialNumber.isBlank()) {
                throw new IllegalArgumentException("Serial number must not be blank");
            }
            if (hardwareId == null || hardwareId.isBlank()) {
                throw new IllegalArgumentException("Hardware ID must not be blank");
            }
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("API key must not be blank");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name must not be blank");
            }
        }
    }
}
