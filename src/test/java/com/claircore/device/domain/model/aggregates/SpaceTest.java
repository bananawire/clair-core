package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaceTest {

    @Test
    void shouldUpdateNameWhenRequested() {
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(UUID.randomUUID()));

        space.updateName("Kitchen");

        assertEquals("Kitchen", space.getName());
    }
}
