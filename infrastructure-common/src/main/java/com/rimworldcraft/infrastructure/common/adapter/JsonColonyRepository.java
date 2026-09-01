package com.rimworldcraft.infrastructure.common.adapter;

import com.rimworldcraft.core.colony.Colony;
import com.rimworldcraft.core.repository.IColonyRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory aggregate index with JSON save lifecycle integration. */
public final class JsonColonyRepository implements IColonyRepository {
    private final JsonSaveAdapter saveAdapter;
    private final Map<UUID, Colony> colonies = new ConcurrentHashMap<>();
    /** Creates a repository using the supplied save adapter. */ public JsonColonyRepository(JsonSaveAdapter saveAdapter) { this.saveAdapter = Objects.requireNonNull(saveAdapter); }
    /** Finds a colony by identifier. */ public Optional<Colony> findById(UUID id) { return Optional.ofNullable(colonies.get(id)); }
    /** Saves and indexes a colony. */ public void save(Colony colony) { colonies.put(colony.getColonyId(), colony); saveAdapter.saveColony(colony.getColonyId()); }
    /** Deletes a colony. */ public void delete(UUID id) { colonies.remove(id); saveAdapter.deleteColony(id); }
    /** Returns active colonies. */ public List<Colony> findAllActive() { return colonies.values().stream().filter(Colony::isActive).toList(); }
}
