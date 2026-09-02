package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.ResourceType;
import com.rimworldcraft.core.api.types.WorldId;

/** Performs a resource operation through the Colony application boundary. */
public interface ResourceOperationUseCase {
    /** Applies the requested operation atomically. */
    void execute(ResourceOperation command);

    /** Immutable resource operation command. */
    record ResourceOperation(WorldId worldId, ColonyId colonyId, ResourceType resourceType, int amount, Operation operation) {
        /** Supported resource operations. */
        public enum Operation { ADD, REMOVE, RESERVE }
    }
}
