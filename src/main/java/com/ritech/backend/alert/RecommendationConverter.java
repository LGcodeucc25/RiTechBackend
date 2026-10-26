package com.ritech.backend.alert;

import com.ritech.backend.analysis.Recommendation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RecommendationConverter implements AttributeConverter<Recommendation, String> {

    @Override
    public String convertToDatabaseColumn(Recommendation recommendation) {
        return recommendation == null ? null : recommendation.databaseValue();
    }

    @Override
    public Recommendation convertToEntityAttribute(String value) {
        return value == null ? null : Recommendation.fromDatabaseValue(value);
    }
}
