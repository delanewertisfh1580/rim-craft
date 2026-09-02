package com.rimworldcraft.core.world;

/** Snapshot of climate inputs needed by Core policies. */
public record ClimateFacts(String climateId, int temperatureCelsius, int humidityPercent, int rainfallPercent) {
    public ClimateFacts {
        if (climateId == null || climateId.isBlank()) throw new IllegalArgumentException("climateId");
        if (humidityPercent < 0 || humidityPercent > 100 || rainfallPercent < 0 || rainfallPercent > 100) {
            throw new IllegalArgumentException("climate percentages must be 0..100");
        }
    }
}
