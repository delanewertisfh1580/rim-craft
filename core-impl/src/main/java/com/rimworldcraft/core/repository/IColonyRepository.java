package com.rimworldcraft.core.repository;

import com.rimworldcraft.core.colony.Colony;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository contract for colony aggregates. */
public interface IColonyRepository {
    /** Finds a colony by identifier. */
    Optional<Colony> findById(UUID id);
    /** Persists a colony. */
    void save(Colony colony);
    /** Removes a colony by identifier. */
    void delete(UUID id);
    /** Returns all active colonies. */
    List<Colony> findAllActive();
}
