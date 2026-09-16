package com.claircore.device.infrastructure.provisioning;

import com.claircore.device.application.commandservices.DeviceCommandService;
import com.claircore.device.domain.model.commands.ImportDevicesCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads factory inventory from {@code DEVICE_PROVISIONING_IMPORT_PATH} on every start when the
 * variable is set. Idempotent: rows already present are skipped by the command handler.
 */
@Component
public class InventoryCsvImportOnStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryCsvImportOnStartup.class);

    private final DeviceCommandService deviceCommandService;
    private final String importPath;

    public InventoryCsvImportOnStartup(DeviceCommandService deviceCommandService,
                                       @Value("${DEVICE_PROVISIONING_IMPORT_PATH:}") String importPath) {
        this.deviceCommandService = deviceCommandService;
        this.importPath = importPath;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (importPath == null || importPath.isBlank()) {
            return;
        }
        Path path = Path.of(importPath);
        if (!Files.isRegularFile(path)) {
            LOGGER.warn("DEVICE_PROVISIONING_IMPORT_PATH={} does not exist; no inventory imported", path);
            return;
        }
        try {
            var records = InventoryCsv.read(path);
            var created = deviceCommandService.handle(new ImportDevicesCommand(records));
            LOGGER.info("Inventory import from {}: {} row(s) read, {} device(s) created", path, records.size(), created.size());
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("Inventory import from {} failed: {}", path, e.getMessage());
        }
    }
}
