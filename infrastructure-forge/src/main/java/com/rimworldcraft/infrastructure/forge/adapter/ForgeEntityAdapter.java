package com.rimworldcraft.infrastructure.forge.adapter;

import com.rimworldcraft.core.api.ports.IEntitySpawnPort;
import com.rimworldcraft.core.api.types.Position;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Forge entity projection adapter; actual EntityType spawning is platform-wired later. */
public final class ForgeEntityAdapter implements IEntitySpawnPort {
    private final Object level;
    private final ConcurrentMap<UUID, Position> positions = new ConcurrentHashMap<>();
    /** Creates an adapter for a Forge level instance. */ public ForgeEntityAdapter(Object level) { this.level = java.util.Objects.requireNonNull(level); }
    /** Registers or spawns a citizen projection. */ public void spawnCitizen(UUID citizenId, Position pos) { positions.put(java.util.Objects.requireNonNull(citizenId), java.util.Objects.requireNonNull(pos)); }
    /** Registers an enemy projection. */ public void spawnEnemy(UUID enemyId, Position pos) { positions.put(java.util.Objects.requireNonNull(enemyId), java.util.Objects.requireNonNull(pos)); }
    /** Registers a trader projection. */ public void spawnTrader(UUID traderId, Position pos) { positions.put(java.util.Objects.requireNonNull(traderId), java.util.Objects.requireNonNull(pos)); }
    /** Removes an entity projection. */ public void despawnEntity(UUID entityId) { positions.remove(entityId); }
    /** Returns the last known entity position. */ public Optional<Position> getEntityPosition(UUID entityId) { return Optional.ofNullable(positions.get(entityId)); }
    /** Returns the backing platform object. */ public Object level() { return level; }
}
