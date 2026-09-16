package com.claircore.device.infrastructure.persistence.jpa.converters;

import com.claircore.device.domain.model.valueobjects.HardwareId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HardwareIdPersistenceConverter implements AttributeConverter<HardwareId, String> {

    @Override
    public String convertToDatabaseColumn(HardwareId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public HardwareId convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new HardwareId(dbData);
    }
}
