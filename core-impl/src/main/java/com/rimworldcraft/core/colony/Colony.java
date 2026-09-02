package com.rimworldcraft.core.colony;

import com.rimworldcraft.core.api.events.DomainEvent;
import com.rimworldcraft.core.api.events.ResourceUpdatedEvent;
import com.rimworldcraft.core.api.exceptions.InsufficientResourcesException;
import com.rimworldcraft.core.api.types.CitizenId;
import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.ResourceType;
import com.rimworldcraft.core.api.types.WorldId;
import com.rimworldcraft.core.shared.ColonyName;
import com.rimworldcraft.core.shared.GridPosition;
import java.util.*;

/** Colony aggregate root; platform-neutral and persistence-free. */
public final class Colony {
    private final ColonyId colonyId;
    private final WorldId worldId;
    private ColonyName colonyName;
    private final Map<ResourceType, Integer> resources = new EnumMap<>(ResourceType.class);
    private final List<Zone> zones = new ArrayList<>();
    private final Set<CitizenId> citizenIds = new LinkedHashSet<>();
    private long value;
    private float threatLevel;
    private boolean active = true;

    /** Creates a legacy colony in the supplied world. */
    public Colony(UUID colonyId, String name) {
        this(new WorldId(new UUID(0L, 0L)), new ColonyId(colonyId), new ColonyName(name));
    }

    /** Creates a typed, world-scoped colony. */
    public Colony(WorldId worldId, ColonyId colonyId, ColonyName name) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.colonyId = Objects.requireNonNull(colonyId, "colonyId");
        this.colonyName = Objects.requireNonNull(name, "name");
    }

    /** Returns the typed colony identifier. */
    public ColonyId id() { return colonyId; }
    /** Returns the typed world scope. */
    public WorldId worldId() { return worldId; }
    /** Returns the colony identifier for legacy callers. */
    public UUID getColonyId() { return colonyId.value(); }
    /** Returns the colony name. */
    public String getName() { return colonyName.value(); }
    /** Renames this colony. */
    public void rename(ColonyName name) { requireActive(); this.colonyName = Objects.requireNonNull(name, "name"); }
    /** Returns an immutable resource snapshot. */
    public Map<ResourceType, Integer> getResources() { return Map.copyOf(resources); }
    /** Returns an immutable zone snapshot. */
    public List<Zone> getZones() { return List.copyOf(zones); }
    /** Returns typed citizen membership. */
    public Set<CitizenId> citizenIds() { return Set.copyOf(citizenIds); }
    /** Returns legacy citizen UUID membership. */
    public List<UUID> getCitizenIds() { return citizenIds.stream().map(CitizenId::value).toList(); }
    /** Returns current value. */
    public long getValue() { return value; }
    /** Returns current threat level. */
    public float getThreatLevel() { return threatLevel; }
    /** Returns whether the colony is active. */
    public boolean isActive() { return active; }
    /** Adds resources. */
    public List<DomainEvent> addResource(ResourceType type, int amount) { requireActive(); requirePositive(amount); resources.merge(Objects.requireNonNull(type), amount, Math::addExact); return List.of(new ResourceUpdatedEvent(getColonyId())); }
    /** Removes resources atomically. */
    public List<DomainEvent> removeResource(ResourceType type, int amount) { requireActive(); requirePositive(amount); Objects.requireNonNull(type); int current = resources.getOrDefault(type, 0); if (current < amount) throw new InsufficientResourcesException("Not enough " + type); if (current == amount) resources.remove(type); else resources.put(type, current - amount); return List.of(new ResourceUpdatedEvent(getColonyId())); }
    /** Adds membership and rejects duplicates. */
    public List<DomainEvent> addCitizen(CitizenId citizenId) { requireActive(); if (!citizenIds.add(Objects.requireNonNull(citizenId))) throw new IllegalArgumentException("Citizen already belongs to colony"); return List.of(new com.rimworldcraft.core.api.events.CitizenJoinedEvent(getColonyId())); }
    /** Legacy membership overload. */
    public List<DomainEvent> addCitizen(UUID citizenId) { return addCitizen(new CitizenId(citizenId)); }
    /** Removes membership idempotently. */
    public List<DomainEvent> removeCitizen(CitizenId citizenId) { requireActive(); if (!citizenIds.remove(Objects.requireNonNull(citizenId))) return List.of(); return List.of(new com.rimworldcraft.core.api.events.CitizenLeftEvent(getColonyId())); }
    /** Legacy membership overload. */
    public List<DomainEvent> removeCitizen(UUID citizenId) { return removeCitizen(new CitizenId(citizenId)); }
    /** Adds a non-overlapping zone. */
    public List<DomainEvent> addZone(Zone zone) { requireActive(); Objects.requireNonNull(zone); if (zones.stream().anyMatch(existing -> existing.center().distance(zone.center()) <= existing.radius() + zone.radius())) throw new IllegalArgumentException("Zone overlaps existing zone"); zones.add(zone); return List.of(new com.rimworldcraft.core.api.events.ZoneAddedEvent(getColonyId())); }
    /** Removes a zone idempotently. */
    public List<DomainEvent> removeZone(UUID zoneId) { requireActive(); boolean removed = zones.removeIf(zone -> zone.id().equals(Objects.requireNonNull(zoneId))); return removed ? List.of(new com.rimworldcraft.core.api.events.ZoneRemovedEvent(getColonyId())) : List.of(); }
    /** Recalculates the colony value. */
    public long recalculateValue() { value = resources.values().stream().mapToLong(Integer::longValue).sum() + zones.size() * 100L + citizenIds.size() * 50L; return value; }
    /** Recalculates bounded threat. */
    public float updateThreatLevel() { threatLevel = Math.min(1.0f, citizenIds.size() * 0.05f); return threatLevel; }
    /** Marks the colony destroyed. */
    public void deactivate() { active = false; }
    private void requireActive() { if (!active) throw new IllegalStateException("Colony is not active"); }
    private static void requirePositive(int amount) { if (amount <= 0) throw new IllegalArgumentException("amount must be positive"); }
}
