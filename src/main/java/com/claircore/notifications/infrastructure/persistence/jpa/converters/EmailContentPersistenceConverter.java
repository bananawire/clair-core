package com.claircore.notifications.infrastructure.persistence.jpa.converters;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmailContentPersistenceConverter implements AttributeConverter<EmailContent, String> {

    @Override
    public String convertToDatabaseColumn(EmailContent attribute) {
        return attribute == null ? null : attribute.html();
    }

    @Override
    public EmailContent convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new EmailContent(dbData);
    }
}
