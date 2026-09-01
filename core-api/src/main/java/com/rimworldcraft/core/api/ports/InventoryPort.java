package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.ResourceType;

/** Typed resource boundary for colony application services. */
public interface InventoryPort {
    /** Returns the currently available quantity. */
    int available(ColonyId colonyId, ResourceType resourceType);
}
