package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Converts external UUID representations at an adapter/application boundary. */
public final class ExternalIdMapper {
    private ExternalIdMapper() { }

    /** Maps an external UUID to a canonical world identifier. */
    public static WorldId worldId(UUID value) { return WorldId.fromUuid(value); }
    /** Maps an external UUID to a canonical colony identifier. */
    public static ColonyId colonyId(UUID value) { return ColonyId.fromUuid(value); }
    /** Maps an external UUID to a canonical citizen identifier. */
    public static CitizenId citizenId(UUID value) { return CitizenId.fromUuid(value); }
    /** Maps an external UUID to a canonical player identifier. */
    public static PlayerId playerId(UUID value) { return PlayerId.fromUuid(value); }
    /** Maps an external UUID to a canonical region identifier. */
    public static RegionId regionId(UUID value) { return RegionId.fromUuid(value); }
    /** Maps an external UUID to a canonical incident identifier. */
    public static IncidentId incidentId(UUID value) { return IncidentId.fromUuid(value); }
    /** Maps an external UUID to a canonical command identifier. */
    public static CommandId commandId(UUID value) { return CommandId.fromUuid(value); }

    /** Parses a UUID at an external string boundary. */
    public static UUID uuid(String value) {
        return UUID.fromString(Objects.requireNonNull(value, "value"));
    }
}
