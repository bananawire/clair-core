package com.claircore.device.infrastructure.persistence.jpa.converters;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApiKeyPersistenceConverter implements AttributeConverter<ApiKey, String> {

    @Override
    public String convertToDatabaseColumn(ApiKey attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ApiKey convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new ApiKey(dbData);
    }
}
