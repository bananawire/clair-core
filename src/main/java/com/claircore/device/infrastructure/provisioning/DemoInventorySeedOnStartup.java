package com.claircore.device.infrastructure.provisioning;

import com.claircore.device.application.commandservices.DeviceCommandService;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.commands.ImportDevicesCommand;
import com.claircore.device.domain.model.commands.ImportDevicesCommand.DeviceProvisioningRecord;
import com.claircore.device.domain.model.queries.GetProvisionedDevicesQuery;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo inventory: five units with predictable hardware ids ({@code CLAIR-0001}..{@code CLAIR-0005})
 * so firmware can be flashed before the first start, and freshly generated API keys. The keys are
 * written to {@code DEVICE_PROVISIONING_EXPORT_PATH}; they are never logged. Only under the
 * {@code demo} profile: an ordinary deployment gets its inventory from the CSV import.
 */
@Component
@Profile("demo")
public class DemoInventorySeedOnStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoInventorySeedOnStartup.class);
    static final int DEMO_DEVICE_COUNT = 5;

    private final DeviceCommandService deviceCommandService;
    private final DeviceQueryService deviceQueryService;
    private final String exportPath;

    public DemoInventorySeedOnStartup(DeviceCommandService deviceCommandService,
                                      DeviceQueryService deviceQueryService,
                                      @Value("${DEVICE_PROVISIONING_EXPORT_PATH:provisioned-devices.csv}") String exportPath) {
        this.deviceCommandService = deviceCommandService;
        this.deviceQueryService = deviceQueryService;
        this.exportPath = exportPath;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var created = deviceCommandService.handle(new ImportDevicesCommand(demoRecords()));
        LOGGER.info("Demo inventory: {} device(s) created, {} already present",
                created.size(), DEMO_DEVICE_COUNT - created.size());
        var inventory = deviceQueryService.handle(new GetProvisionedDevicesQuery(1000));
        Path path = Path.of(exportPath);
        try {
            InventoryCsv.write(path, inventory);
            LOGGER.info("Demo inventory with API keys written to {}", path.toAbsolutePath());
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("Could not write demo inventory to {}: {}", path.toAbsolutePath(), e.getMessage());
        }
    }

    static List<DeviceProvisioningRecord> demoRecords() {
        List<DeviceProvisioningRecord> records = new ArrayList<>();
        for (int i = 1; i <= DEMO_DEVICE_COUNT; i++) {
            String suffix = String.format("%04d", i);
            records.add(new DeviceProvisioningRecord("SN-" + suffix, "CLAIR-" + suffix,
                    ApiKey.generate().value(), "Sensor " + suffix));
        }
        return records;
    }
}
