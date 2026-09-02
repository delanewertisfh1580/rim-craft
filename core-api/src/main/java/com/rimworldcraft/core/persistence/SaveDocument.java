package com.rimworldcraft.core.persistence;

import com.rimworldcraft.core.shared.SchemaVersion;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Map;
import java.util.Objects;

public record SaveDocument(String format, int formatVersion, String aggregateType, String aggregateId,
                           WorldId worldId, SchemaVersion schemaVersion, AggregateVersion aggregateVersion,
                           long savedAtTick, Map<String,Object> payload, Map<String,String> metadata) {
    public SaveDocument {
        if (!"rwc-save".equals(format)) throw new IllegalArgumentException("unsupported format");
        if (formatVersion < 1) throw new IllegalArgumentException("formatVersion must be positive");
        Objects.requireNonNull(aggregateType); Objects.requireNonNull(aggregateId); Objects.requireNonNull(worldId);
        Objects.requireNonNull(schemaVersion); Objects.requireNonNull(aggregateVersion);
        if (savedAtTick < 0) throw new IllegalArgumentException("savedAtTick must be >= 0");
        payload=Map.copyOf(payload); metadata=Map.copyOf(metadata);
    }
    public SaveDocument(String aggregateType, int schemaVersion, Map<String,Object> payload, Map<String,String> metadata) {
        this("rwc-save",1,aggregateType,"unknown",new WorldId(new java.util.UUID(0,0)),new SchemaVersion(schemaVersion),new AggregateVersion(0),0,payload,metadata);
    }
}
