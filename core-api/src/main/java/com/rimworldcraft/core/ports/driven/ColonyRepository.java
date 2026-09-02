package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.WorldId;
import java.util.List;
import java.util.Optional;

/** Driven persistence port owned by the Colony context. */
public interface ColonyRepository {
    /** Finds a colony within a world scope. */
    Optional<ColonyRecord> findById(WorldId worldId, ColonyId colonyId);
    /** Saves a colony snapshot at the application boundary. */
    ColonyRecord save(ColonyRecord colony);
    /** Deletes a colony within a world scope. */
    void delete(WorldId worldId, ColonyId colonyId);
    /** Lists active colonies in a world scope. */
    List<ColonyRecord> findAllActive(WorldId worldId);

    /** Immutable persistence-neutral colony record. */
    record ColonyRecord(WorldId worldId, ColonyId colonyId, String name, boolean active) { }
}
