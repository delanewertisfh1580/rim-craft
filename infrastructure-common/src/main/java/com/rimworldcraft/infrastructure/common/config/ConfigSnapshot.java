package com.rimworldcraft.infrastructure.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public record ConfigSnapshot(long reloadId, Map<String, JsonNode> documents, String source) {
    public ConfigSnapshot { documents=Map.copyOf(documents); }
    public JsonNode document(String key) { return documents.get(key); }
}
