package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.Position;
import java.util.Optional;
import java.util.UUID;

/** Projects domain actors into the game world. */
public interface IEntitySpawnPort {
    /** Spawns or reconciles a citizen at a position. */
    void spawnCitizen(UUID citizenId, Position pos);
    /** Spawns an enemy at a position. */
    void spawnEnemy(UUID enemyId, Position pos);
    /** Spawns a trader at a position. */
    void spawnTrader(UUID traderId, Position pos);
    /** Removes a projected entity. */
    void despawnEntity(UUID entityId);
    /** Returns the current projected position when available. */
    Optional<Position> getEntityPosition(UUID entityId);
}
