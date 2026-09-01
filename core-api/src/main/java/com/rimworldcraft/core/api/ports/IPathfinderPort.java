package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.Path;
import com.rimworldcraft.core.api.types.PathfinderContext;
import com.rimworldcraft.core.api.types.Position;
import java.util.Optional;
import java.util.UUID;

/** Calculates routes without exposing a concrete navigation library. */
public interface IPathfinderPort {
    /** Finds a route between two positions. */
    Optional<Path> findPath(Position start, Position target, PathfinderContext context);
    /** Cancels an active request associated with an entity. */
    void cancelPath(UUID entityId);
}
