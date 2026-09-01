package com.rimworldcraft.infrastructure.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Validates the common envelope of JSON configuration files. */
public final class ConfigValidator {
    /** Validates JSON syntax and requires a positive schemaVersion field. */
    public boolean validateJson(String json, String schemaPath) {
        try {
            JsonNode node = JsonUtils.mapper().readTree(json);
            return node != null && node.isObject()
                    && node.has("schemaVersion")
                    && node.get("schemaVersion").canConvertToInt()
                    && node.get("schemaVersion").asInt() > 0
                    && (schemaPath == null || schemaPath.isBlank() || Files.exists(Path.of(schemaPath)));
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }
}
