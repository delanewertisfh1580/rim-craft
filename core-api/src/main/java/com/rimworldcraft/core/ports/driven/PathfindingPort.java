package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.api.types.Path;
import com.rimworldcraft.core.api.types.PathfinderContext;
import com.rimworldcraft.core.api.types.WorldId;
import java.util.Optional;

/** Driven navigation capability with no dependency on a pathfinding library. */
public interface PathfindingPort {
    /** Finds a route inside one world scope. */
    Optional<Path> findPath(WorldId worldId, GridPosition start, GridPosition target, PathfinderContext context);
    /** Cancels a route request by external boundary handle. */
    void cancelPath(String requestHandle);
}
