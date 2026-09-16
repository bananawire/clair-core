package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizationTest {

    @Test
    void shouldExposePlanLimitsWhenRequested() {
        Organization organization = new Organization("Home", new UserId(UUID.randomUUID()));

        assertEquals(5, organization.getMaxSpaces());
        assertEquals(10, organization.getMaxDevices());
    }

    @Test
    void shouldUpdateNameWhenRequested() {
        Organization organization = new Organization("Home", new UserId(UUID.randomUUID()));

        organization.updateName("Office");

        assertEquals("Office", organization.getName());
    }
}
