package com.lectuaria.backend.model.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VisibilityConverter implements AttributeConverter<Visibility, String> {

    @Override
    public String convertToDatabaseColumn(Visibility attribute) {
        return attribute != null ? attribute.name().toLowerCase() : null;
    }

    @Override
    public Visibility convertToEntityAttribute(String dbData) {
        return dbData != null ? Visibility.valueOf(dbData.toUpperCase()) : null;
    }
}