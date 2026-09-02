package com.rimworldcraft.infrastructure.common.config;

public record ConfigDiagnostic(String logicalKey, String physicalPath, String jsonPath, String reason, String effectiveFallback, long reloadId) {}
