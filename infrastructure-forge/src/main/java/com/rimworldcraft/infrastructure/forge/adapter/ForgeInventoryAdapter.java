package com.rimworldcraft.infrastructure.forge.adapter;

import com.rimworldcraft.core.api.exceptions.InsufficientResourcesException;
import com.rimworldcraft.core.api.ports.IInventoryPort;
import com.rimworldcraft.core.api.types.ResourceType;
import java.util.EnumMap;
import java.util.Objects;

/** Forge inventory adapter seam backed by a synchronized resource view. */
public final class ForgeInventoryAdapter implements IInventoryPort {
    private final Object inventory;
    private final EnumMap<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
    /** Creates an adapter for a Forge inventory instance. */ public ForgeInventoryAdapter(Object inventory) { this.inventory = Objects.requireNonNull(inventory); }
    /** Returns the item count. */ public synchronized int getItemCount(ResourceType type) { return counts.getOrDefault(Objects.requireNonNull(type), 0); }
    /** Takes items or throws a domain exception. */ public synchronized void takeItems(ResourceType type, int amount) { if (amount <= 0 || !hasItems(type, amount)) throw new InsufficientResourcesException("Not enough " + type); counts.put(type, getItemCount(type) - amount); }
    /** Adds items. */ public synchronized void addItems(ResourceType type, int amount) { if (amount <= 0) throw new IllegalArgumentException("amount must be positive"); counts.merge(Objects.requireNonNull(type), amount, Math::addExact); }
    /** Checks item availability. */ public synchronized boolean hasItems(ResourceType type, int amount) { return amount >= 0 && getItemCount(type) >= amount; }
}
