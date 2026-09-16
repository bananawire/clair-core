package com.claircore.device.infrastructure.provisioning;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.commands.ImportDevicesCommand.DeviceProvisioningRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The one file format factory inventory travels in: {@code serial_number,hardware_id,api_key,name}.
 * No quoting; none of the fields may contain a comma, which the value objects already guarantee for
 * the identifiers and the importer rejects for names.
 */
public final class InventoryCsv {
    public static final String HEADER = "serial_number,hardware_id,api_key,name";

    private InventoryCsv() {
    }

    public static List<DeviceProvisioningRecord> read(Path path) throws IOException {
        List<DeviceProvisioningRecord> records = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#") || (i == 0 && line.equalsIgnoreCase(HEADER))) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException("Line " + (i + 1) + " must have 4 comma-separated fields");
            }
            records.add(new DeviceProvisioningRecord(parts[0].strip(), parts[1].strip(), parts[2].strip(), parts[3].strip()));
        }
        return records;
    }

    public static void write(Path path, List<Device> devices) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Device device : devices) {
            if (device.getName().contains(",")) {
                throw new IllegalArgumentException("Device name must not contain a comma: " + device.getName());
            }
            lines.add(String.join(",", device.getSerialNumber(), device.getHardwareId().value(),
                    device.getApiKey().value(), device.getName()));
        }
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }
}
