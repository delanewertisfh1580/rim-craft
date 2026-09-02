package com.rimworldcraft.core.world;

import com.rimworldcraft.core.ports.driven.WorldSnapshotPort;
import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.Optional;

/** Application-facing World Context service using immutable snapshots only. */
public final class WorldContextService {
    private final WorldSnapshotPort snapshots;

    public WorldContextService(WorldSnapshotPort snapshots) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    }

    public Optional<WorldSnapshot> snapshot(WorldId worldId) {
        return snapshots.snapshot(Objects.requireNonNull(worldId, "worldId"));
    }

    public SettlementValidationResult validateSettlement(WorldId worldId, GridPosition position) {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(position, "position");
        return snapshots.validateSettlement(worldId, position);
    }
}
