package com.rimworldcraft.core.colony;

import com.rimworldcraft.core.api.events.*;
import com.rimworldcraft.core.api.exceptions.InsufficientResourcesException;
import com.rimworldcraft.core.api.types.ResourceType;
import com.rimworldcraft.core.api.types.Position;
import java.util.*;

/** Colony aggregate root; see module-colony-manager.md. */
public final class Colony {
    private final UUID colonyId;
    private final String name;
    private final Map<ResourceType, Integer> resources = new EnumMap<>(ResourceType.class);
    private final List<Zone> zones = new ArrayList<>();
    private final List<UUID> citizenIds = new ArrayList<>();
    private long value;
    private float threatLevel;
    private boolean active = true;

    /** Creates an active empty colony. */
    public Colony(UUID colonyId, String name) {
        this.colonyId = Objects.requireNonNull(colonyId);
        this.name = requireText(name, "name");
    }
    /** Returns the colony identifier. */ public UUID getColonyId() { return colonyId; }
    /** Returns the colony name. */ public String getName() { return name; }
    /** Returns an immutable resource snapshot. */ public Map<ResourceType, Integer> getResources() { return Map.copyOf(resources); }
    /** Returns an immutable zone snapshot. */ public List<Zone> getZones() { return List.copyOf(zones); }
    /** Returns an immutable citizen identifier snapshot. */ public List<UUID> getCitizenIds() { return List.copyOf(citizenIds); }
    /** Returns current calculated value. */ public long getValue() { return value; }
    /** Returns current threat level. */ public float getThreatLevel() { return threatLevel; }
    /** Returns whether the colony is active. */ public boolean isActive() { return active; }
    /** Adds resources and returns the resulting event. */
    public List<DomainEvent> addResource(ResourceType type, int amount) { requirePositive(amount); resources.merge(Objects.requireNonNull(type), amount, Math::addExact); return List.of(new ResourceUpdatedEvent(colonyId)); }
    /** Removes resources and returns the resulting event. */
    public List<DomainEvent> removeResource(ResourceType type, int amount) { requirePositive(amount); Objects.requireNonNull(type); int current = resources.getOrDefault(type, 0); if (current < amount) throw new InsufficientResourcesException("Not enough " + type); if (current == amount) resources.remove(type); else resources.put(type, current - amount); return List.of(new ResourceUpdatedEvent(colonyId)); }
    /** Adds a citizen when not already present. */
    public List<DomainEvent> addCitizen(UUID citizenId) { if (!citizenIds.contains(Objects.requireNonNull(citizenId))) citizenIds.add(citizenId); return List.of(new CitizenJoinedEvent(citizenId)); }
    /** Removes a citizen when present. */
    public List<DomainEvent> removeCitizen(UUID citizenId) { citizenIds.remove(citizenId); return List.of(new CitizenLeftEvent(citizenId)); }
    /** Adds a non-overlapping zone. */
    public List<DomainEvent> addZone(Zone zone) { Objects.requireNonNull(zone); if (zones.stream().anyMatch(existing -> existing.center().distance(zone.center()) <= existing.radius() + zone.radius())) throw new IllegalArgumentException("Zone overlaps existing zone"); zones.add(zone); return List.of(new ZoneAddedEvent(colonyId)); }
    /** Removes a zone by identifier. */
    public List<DomainEvent> removeZone(UUID zoneId) { zones.removeIf(zone -> zone.id().equals(zoneId)); return List.of(new ZoneRemovedEvent(colonyId)); }
    /** Recalculates value from resources, zones, and citizens. */
    public long recalculateValue() { value = resources.values().stream().mapToLong(Integer::longValue).sum() + zones.size() * 100L + citizenIds.size() * 50L; return value; }
    /** Recalculates a bounded threat level. */
    public float updateThreatLevel() { threatLevel = Math.min(1.0f, citizenIds.size() * 0.05f); return threatLevel; }
    /** Marks the colony inactive. */ public void deactivate() { active = false; }
    private static void requirePositive(int amount) { if (amount <= 0) throw new IllegalArgumentException("amount must be positive"); }
    private static String requireText(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank"); return value; }
}
