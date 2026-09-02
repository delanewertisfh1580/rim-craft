package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.UUID;

/** Platform-neutral navigation intent. */
public record PathRequest(UUID requestId, WorldId worldId, CitizenId citizenId,
                          GridPosition start, GridPosition target) {
    public PathRequest {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(citizenId, "citizenId");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(target, "target");
    }

    /** Compatibility constructor for callers that only supplied a target. */
    public PathRequest(UUID requestId, WorldId worldId, CitizenId citizenId, GridPosition target) {
        this(requestId, worldId, citizenId, target, target);
    }
}
