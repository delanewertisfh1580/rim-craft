package com.rimworldcraft.core.factory;

import com.rimworldcraft.core.building.*;
import com.rimworldcraft.core.api.types.Position;
import com.rimworldcraft.core.colony.Colony;
import java.util.UUID;
/** Creates build order aggregates. */
public final class BuildOrderFactory {
    /** Creates an order from a blueprint. */
    public BuildOrder createFromBlueprint(Blueprint blueprint, Colony colony, Position position) { blueprint.validate(); return new BuildOrder(UUID.randomUUID(), colony.getColonyId(), blueprint.id(), position, 5, blueprint.requiredResources()); }
    /** Creates an order from a ghost block. */
    public BuildOrder createFromGhostBlock(GhostBlock ghostBlock) { return new BuildOrder(ghostBlock.buildOrderId(), UUID.randomUUID(), ghostBlock.blockType(), new Position(ghostBlock.position().x(), ghostBlock.position().y(), ghostBlock.position().z()), 5, java.util.Map.of()); }
}
