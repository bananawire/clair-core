package com.claircore.device.infrastructure.persistence.jpa.converters;

import com.claircore.device.domain.model.valueobjects.ClaimToken;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimTokenPersistenceConverter implements AttributeConverter<ClaimToken, String> {

    @Override
    public String convertToDatabaseColumn(ClaimToken attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ClaimToken convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new ClaimToken(dbData);
    }
}
