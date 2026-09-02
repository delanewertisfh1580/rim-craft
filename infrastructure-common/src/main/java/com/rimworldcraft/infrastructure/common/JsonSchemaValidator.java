package com.rimworldcraft.infrastructure.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.InputFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Validates configuration JSON against NetworkNT JSON Schema. */
public final class JsonSchemaValidator {
    private final ObjectMapper mapper;
    public JsonSchemaValidator() { this(com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper()); }
    public JsonSchemaValidator(ObjectMapper mapper) { this.mapper=Objects.requireNonNull(mapper); }
    public List<String> validate(String json, String schemaJson) {
        try {
            JsonSchema schema=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);
            return schema.validate(json, InputFormat.JSON).stream().map(Object::toString).collect(Collectors.toList());
        } catch (RuntimeException exception) { return List.of(exception.getMessage() == null ? "schema validation failed" : exception.getMessage()); }
    }
    public void validate(Path config, Path schema) {
        if(config==null||schema==null) throw new IllegalArgumentException("paths required");
        try { List<String> errors=validate(Files.readString(config),Files.readString(schema)); if(!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ",errors)); }
        catch(IOException exception){throw new IllegalArgumentException("unable to read configuration or schema",exception);}
    }
}
