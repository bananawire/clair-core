package com.claircore.notifications.infrastructure.persistence.jpa.converters;

import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmailSubjectPersistenceConverter implements AttributeConverter<EmailSubject, String> {

    @Override
    public String convertToDatabaseColumn(EmailSubject attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public EmailSubject convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new EmailSubject(dbData);
    }
}
