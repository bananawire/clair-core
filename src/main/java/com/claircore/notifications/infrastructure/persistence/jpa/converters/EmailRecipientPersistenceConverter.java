package com.claircore.notifications.infrastructure.persistence.jpa.converters;

import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmailRecipientPersistenceConverter implements AttributeConverter<EmailRecipient, String> {

    @Override
    public String convertToDatabaseColumn(EmailRecipient attribute) {
        return attribute == null ? null : attribute.address();
    }

    @Override
    public EmailRecipient convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new EmailRecipient(dbData);
    }
}
