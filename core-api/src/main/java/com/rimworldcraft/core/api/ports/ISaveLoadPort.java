package com.rimworldcraft.core.api.ports;

import java.util.UUID;

/**
 * Legacy persistence boundary retained for compatibility.
 *
 * @deprecated Use a context-owned driven repository or snapshot port. Core
 * aggregates must not expose NBT or persistence methods.
 */
@Deprecated(forRemoval = false)
public interface ISaveLoadPort {
    /** Saves one colony by external compatibility identifier. */
    void saveColony(UUID colonyId);
    /** Loads one colony by external compatibility identifier. */
    void loadColony(UUID colonyId);
    /** Returns whether a colony save exists. */
    boolean exists(UUID colonyId);
    /** Deletes one colony save. */
    void deleteColony(UUID colonyId);
    /** Saves all pending state. */
    void saveAll();
    /** Loads all available state. */
    void loadAll();
}
