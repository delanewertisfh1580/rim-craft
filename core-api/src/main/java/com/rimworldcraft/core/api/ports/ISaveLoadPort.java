package com.rimworldcraft.core.api.ports;

import java.util.UUID;

/** Abstracts world save and load operations. */
public interface ISaveLoadPort {
    /** Saves one colony by identifier. */
    void saveColony(UUID colonyId);
    /** Loads one colony by identifier. */
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
