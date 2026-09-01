package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.ResourceType;

/** Provides inventory operations through a platform-neutral contract. */
public interface IInventoryPort {
    /** Returns the current item count. */
    int getItemCount(ResourceType type);
    /** Removes items or throws a domain exception when unavailable. */
    void takeItems(ResourceType type, int amount);
    /** Adds items to the inventory. */
    void addItems(ResourceType type, int amount);
    /** Returns whether at least the requested amount exists. */
    boolean hasItems(ResourceType type, int amount);
}
