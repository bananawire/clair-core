package com.claircore.device.domain.model.commands;

import org.junit.jupiter.api.Test;

import com.claircore.device.domain.model.valueobjects.UserId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class UpdateOrganizationNameCommandTest {

    @Test
    void shouldCreateCommandWhenValuesAreValid() {
        UUID organizationId = UUID.fromString("550e8400-e29b-41d4-a716-446655447002");

        UpdateOrganizationNameCommand command = new UpdateOrganizationNameCommand(organizationId, "Office", new UserId(UUID.randomUUID()));

        assertEquals(organizationId, command.organizationId());
        assertEquals("Office", command.name());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new UpdateOrganizationNameCommand(UUID.randomUUID(), " ", new UserId(UUID.randomUUID()))
        );

        assertEquals("Name must not be null or blank", exception.getMessage());
    }
}
