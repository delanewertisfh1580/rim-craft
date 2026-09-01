package com.rimworldcraft.infrastructure.forge;

import com.rimworldcraft.core.api.*;
import java.util.*;

/** Forge world adapter seam; see hexagonal-architecture.md. */
public final class ForgeBlockAdapter implements IBlockWorldPort {
    public BlockType getBlockType(Position position) { return BlockType.AIR; }
    public void setBlock(Position position, BlockType blockType) { }
    public boolean isAir(Position position) { return getBlockType(position) == BlockType.AIR; }
}
/** Forge entity adapter seam; see entity-integration.md. */
public final class ForgeEntityAdapter implements IEntitySpawnPort {
    public void spawnCitizen(UUID citizenId, Position position) { }
    public void despawnEntity(UUID entityId) { }
}
/** Baritone integration seam; see pathfinding-layer.md. */
public final class BaritonePathfinderAdapter implements IPathfinderPort {
    public Optional<Path> findPath(Position start, Position target, EntityTraversalContext context) {
        if (start.equals(target)) return Optional.of(Path.empty());
        return Optional.empty();
    }
}
/** Save adapter seam; see save-serialization.md. */
public final class NbtSaveAdapter implements ISaveLoadPort {
    private final Set<UUID> saved = new HashSet<>();
    public void saveColony(UUID colonyId) { saved.add(colonyId); }
    public Optional<UUID> loadColony(UUID colonyId) { return Optional.ofNullable(saved.contains(colonyId) ? colonyId : null); }
    public boolean exists(UUID colonyId) { return saved.contains(colonyId); }
    public void deleteColony(UUID colonyId) { saved.remove(colonyId); }
}
/** In-memory event bus used by the bootstrap and tests. */
public final class InMemoryEventBus implements IEventBusPort {
    private final Map<Class<?>, List<java.util.function.Consumer<?>>> handlers = new HashMap<>();
    public void publish(Object event) { handlers.getOrDefault(event.getClass(), List.of()).forEach(handler -> invoke(handler, event)); }
    public <T> void subscribe(Class<T> type, java.util.function.Consumer<T> handler) { handlers.computeIfAbsent(type, ignored -> new ArrayList<>()).add(handler); }
    public <T> void unsubscribe(Class<T> type, java.util.function.Consumer<T> handler) { handlers.getOrDefault(type, List.of()).remove(handler); }
    @SuppressWarnings("unchecked") private static <T> void invoke(java.util.function.Consumer<?> handler, Object event) { ((java.util.function.Consumer<T>) handler).accept((T) event); }
}
/** Mod composition root placeholder. */
public final class RimWorldCraftMod { public void onInitialize() { } }
