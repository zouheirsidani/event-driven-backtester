package com.backtester.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * JPA {@link AttributeConverter} that serialises a {@code List<String>} to a JSON string
 * for storage in a database column, and deserialises it back on read.
 *
 * <p>Note: This converter is declared but not currently in active use — the {@code tickers}
 * field on {@link com.backtester.infrastructure.persistence.entity.BacktestRunEntity} uses
 * Hibernate's native {@code @JdbcTypeCode(SqlTypes.JSON)} instead, which is more robust for
 * PostgreSQL JSONB columns.  This converter remains as an alternative for other contexts.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Serialises a list of strings to a JSON array string for database storage.
     *
     * @param list List to serialise; may be null.
     * @return JSON string representation, or null if input is null.
     */
    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null) return null;
        try {
            return MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize list to JSON", e);
        }
    }

    /**
     * Deserialises a JSON array string from the database back to a {@code List<String>}.
     *
     * @param json JSON string from the database; may be null.
     * @return Parsed list, or null if input is null.
     */
    @Override
    public List<String> convertToEntityAttribute(String json) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize list from JSON", e);
        }
    }
}
