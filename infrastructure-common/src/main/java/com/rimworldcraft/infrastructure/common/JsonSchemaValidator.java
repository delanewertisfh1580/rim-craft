package com.rimworldcraft.infrastructure.common;

import java.nio.file.Path;

/** Configuration validation seam; see configuration-mutation-testing.md. */
public final class JsonSchemaValidator {
    public void validate(Path config, Path schema) { if (config == null || schema == null) throw new IllegalArgumentException("paths required"); }
}
