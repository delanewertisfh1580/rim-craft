package com.rimworldcraft.core.shared;

/** Canonical positive contract schema version. */
public record SchemaVersion(int value) {
    /** Creates a schema version. */
    public SchemaVersion { if (value <= 0) throw new IllegalArgumentException("SchemaVersion must be positive"); }
    /** Converts this version to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.SchemaVersion toApiType() { return new com.rimworldcraft.core.api.types.SchemaVersion(value); }
}
