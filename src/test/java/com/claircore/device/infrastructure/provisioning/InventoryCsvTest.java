package com.claircore.device.infrastructure.provisioning;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryCsvTest {
    @TempDir
    Path directory;

    @Test
    void writesAndReadsTheSameInventory() throws Exception {
        var device = new Device("SN-0001", "Sensor 0001", new HardwareId("CLAIR-0001"), ApiKey.generate(), new DeviceType("air-quality-v1"));
        Path file = directory.resolve("nested/inventory.csv");
        InventoryCsv.write(file, List.of(device));
        assertThat(Files.readAllLines(file)).hasSize(2).first().isEqualTo(InventoryCsv.HEADER);
        var records = InventoryCsv.read(file);
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.serialNumber()).isEqualTo("SN-0001");
            assertThat(record.hardwareId()).isEqualTo("CLAIR-0001");
            assertThat(record.apiKey()).isEqualTo(device.getApiKey().value());
            assertThat(record.name()).isEqualTo("Sensor 0001");
        });
    }

    @Test
    void skipsCommentsAndBlankLinesButRejectsMalformedRows() throws Exception {
        Path file = directory.resolve("inventory.csv");
        Files.writeString(file, InventoryCsv.HEADER + "\n# factory batch 3\n\nSN-0002,CLAIR-0002,k2,Sensor 0002\n");
        assertThat(InventoryCsv.read(file)).hasSize(1);
        Files.writeString(file, "SN-0003,CLAIR-0003\n");
        assertThatThrownBy(() -> InventoryCsv.read(file)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 comma-separated fields");
    }

    @Test
    void demoRecordsAreFivePredictableUnitsWithDistinctKeys() {
        var records = DemoInventorySeedOnStartup.demoRecords();
        assertThat(records).extracting(r -> r.hardwareId())
                .containsExactly("CLAIR-0001", "CLAIR-0002", "CLAIR-0003", "CLAIR-0004", "CLAIR-0005");
        assertThat(records).extracting(r -> r.apiKey()).doesNotHaveDuplicates();
    }
}
