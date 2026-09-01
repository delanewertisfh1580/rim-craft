package com.rimworldcraft.infrastructure.forge.adapter;

import com.rimworldcraft.core.api.ports.IPathfinderPort;
import com.rimworldcraft.core.api.types.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Baritone integration seam; replace the backend call when Baritone is wired. */
public final class BaritonePathfinderAdapter implements IPathfinderPort {
    private final Object entity;
    private final Set<UUID> cancelled = ConcurrentHashMap.newKeySet();
    /** Creates an adapter for a platform entity. */ public BaritonePathfinderAdapter(Object entity) { this.entity = Objects.requireNonNull(entity); }
    /** Returns a direct route only when start and target are equal; Baritone integration is pending. */
    @Override public Optional<Path> findPath(Position start, Position target, PathfinderContext context) { Objects.requireNonNull(start); Objects.requireNonNull(target); Objects.requireNonNull(context); if (start.equals(target)) return Optional.of(new Path(List.of(new Waypoint(start, false, false)), 0, 0)); return Optional.empty(); }
    /** Cancels a request associated with an entity identifier. */ @Override public void cancelPath(UUID entityId) { cancelled.add(Objects.requireNonNull(entityId)); }
    /** Returns the platform entity handle. */ public Object entity() { return entity; }
}
