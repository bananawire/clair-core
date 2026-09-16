package com.claircore.device.infrastructure.persistence.jpa.converters;

import com.claircore.device.domain.model.valueobjects.DeviceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeviceTypePersistenceConverter implements AttributeConverter<DeviceType, String> {

    @Override
    public String convertToDatabaseColumn(DeviceType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public DeviceType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new DeviceType(dbData);
    }
}
