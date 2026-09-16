package com.claircore.iam.infrastructure.persistence.jpa.converters;

import com.claircore.iam.domain.model.valueobjects.Password;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PasswordPersistenceConverter implements AttributeConverter<Password, String> {

    @Override
    public String convertToDatabaseColumn(Password attribute) {
        return attribute == null ? null : attribute.passwordHash();
    }

    @Override
    public Password convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new Password(dbData);
    }
}
