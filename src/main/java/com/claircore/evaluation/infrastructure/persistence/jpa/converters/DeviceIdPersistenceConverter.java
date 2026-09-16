package com.claircore.evaluation.infrastructure.persistence.jpa.converters;

import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

@Converter(autoApply = true)
public class DeviceIdPersistenceConverter implements AttributeConverter<DeviceId, UUID> {

    @Override
    public UUID convertToDatabaseColumn(DeviceId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public DeviceId convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : new DeviceId(dbData);
    }
}
