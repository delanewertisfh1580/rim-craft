package com.rimworldcraft.infrastructure.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** JSON serialization utility with deterministic ISO date handling. */
public final class JsonUtils {
    private static final ObjectMapper MAPPER = createMapper();
    private JsonUtils() { }
    /** Serializes an object to JSON. */
    public static String toJson(Object object) { try { return MAPPER.writeValueAsString(object); } catch (Exception exception) { throw new IllegalStateException("Cannot serialize JSON", exception); } }
    /** Deserializes JSON into the requested type. */
    public static <T> T fromJson(String json, Class<T> type) { try { return MAPPER.readValue(json, type); } catch (Exception exception) { throw new IllegalArgumentException("Cannot parse JSON", exception); } }
    /** Returns the shared configured mapper. */
    public static ObjectMapper mapper() { return MAPPER; }
    private static ObjectMapper createMapper() { ObjectMapper mapper = new ObjectMapper(); mapper.registerModule(new JavaTimeModule()); mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); return mapper; }
}
