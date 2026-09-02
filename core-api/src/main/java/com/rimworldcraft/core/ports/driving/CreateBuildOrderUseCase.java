package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.api.types.BuildOrderStatus;
import com.rimworldcraft.core.api.types.CitizenId;
import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.ContentId;
import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.api.types.WorldId;

/** Creates a building order through the Building application boundary. */
public interface CreateBuildOrderUseCase {
    /** Creates a pending build order and returns its identifier. */
    ContentId create(CreateBuildOrderCommand command);

    /** Immutable build-order input using summaries and typed IDs. */
    record CreateBuildOrderCommand(WorldId worldId, ColonyId colonyId, ContentId blueprintId,
                                   GridPosition position, CitizenId requestedBy, BuildOrderStatus initialStatus) { }
}
