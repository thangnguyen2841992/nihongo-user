package com.thang.nihongo_user.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AttemptSnapshotConverter implements AttributeConverter<AttemptSnapshot, String> {
    private static final ObjectMapper JSON = new ObjectMapper();
    public String convertToDatabaseColumn(AttemptSnapshot value) {
        if (value == null) return null;
        try { return JSON.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot save attempt snapshot", e); }
    }
    public AttemptSnapshot convertToEntityAttribute(String value) {
        if (value == null) return null;
        try { return JSON.readValue(value, AttemptSnapshot.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot read attempt snapshot", e); }
    }
}
