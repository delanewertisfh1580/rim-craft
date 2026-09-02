package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.ResourceType;

/** Provides colony resource availability without exposing a persistence adapter. */
public interface InventoryPort {
    /** Returns the quantity available for a colony resource. */
    int available(ColonyId colonyId, ResourceType resourceType);
}
