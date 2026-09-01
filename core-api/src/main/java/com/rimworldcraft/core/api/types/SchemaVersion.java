package com.rimworldcraft.core.api.types;

/** Version of a persisted or published contract schema. */
public record SchemaVersion(int value) {
    /** Creates a schema version. */
    public SchemaVersion {
        if (value <= 0) throw new IllegalArgumentException("SchemaVersion must be positive");
    }
}
