package com.claircore.device.domain.model.commands;

import org.junit.jupiter.api.Test;

import com.claircore.device.domain.model.valueobjects.UserId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class UpdateSpaceNameCommandTest {

    @Test
    void shouldCreateCommandWhenValuesAreValid() {
        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655447021");

        UpdateSpaceNameCommand command = new UpdateSpaceNameCommand(spaceId, "Bedroom", new UserId(UUID.randomUUID()));

        assertEquals(spaceId, command.spaceId());
        assertEquals("Bedroom", command.name());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new UpdateSpaceNameCommand(UUID.randomUUID(), " ", new UserId(UUID.randomUUID()))
        );

        assertEquals("Name must not be null or blank", exception.getMessage());
    }
}
