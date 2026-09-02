package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.UUID;

/** Requests Building-owned work without exposing a Building aggregate to Goal AI. */
public interface BuildTaskIntentPort {
    void submit(BuildTaskIntent intent);

    record BuildTaskIntent(UUID taskId, UUID buildOrderId, WorldId worldId,
                           CitizenId citizenId, GridPosition target) {
        public BuildTaskIntent {
            Objects.requireNonNull(taskId, "taskId");
            Objects.requireNonNull(buildOrderId, "buildOrderId");
            Objects.requireNonNull(worldId, "worldId");
            Objects.requireNonNull(citizenId, "citizenId");
            Objects.requireNonNull(target, "target");
        }
    }
}
