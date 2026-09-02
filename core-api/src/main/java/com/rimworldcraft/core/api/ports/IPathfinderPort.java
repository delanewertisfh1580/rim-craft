package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.Path;
import com.rimworldcraft.core.api.types.PathfinderContext;
import com.rimworldcraft.core.api.types.Position;
import java.util.Optional;
import java.util.UUID;

/**
 * Legacy compatibility pathfinding port.
 *
 * @deprecated Use {@code com.rimworldcraft.core.ports.driven.PathfindingPort}
 * with {@code WorldId} and {@code GridPosition}.
 */
@Deprecated(forRemoval = false)
public interface IPathfinderPort {
    /** Finds a route between legacy positions. */
    Optional<Path> findPath(Position start, Position target, PathfinderContext context);
    /** Cancels a request associated with an external entity identifier. */
    void cancelPath(UUID entityId);
}
