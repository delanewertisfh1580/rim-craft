package com.rimworldcraft.core.factory;

import com.rimworldcraft.core.building.Blueprint;
import com.rimworldcraft.core.api.types.ResourceType;
import java.util.Map;
/** Creates blueprint definitions. */
public final class BlueprintFactory {
    /** Creates a default blueprint from a prefab identifier. */
    public Blueprint createFromPrefab(String prefabId) { return new Blueprint(prefabId, prefabId, "Generated prefab", 1, 1, 1, Map.of(ResourceType.WOOD, 1), 20); }
    /** Creates a blueprint from a platform-neutral selection. */
    public Blueprint createFromPlayerSelection(Selection selection) { return new Blueprint("player-selection", "Player selection", "Generated selection", selection.width(), selection.height(), selection.depth(), Map.of(ResourceType.WOOD, selection.width() * selection.height() * selection.depth()), 20); }
    /** Minimal player selection DTO. */
    public record Selection(int width, int height, int depth) { public Selection { if (width <= 0 || height <= 0 || depth <= 0) throw new IllegalArgumentException("Dimensions must be positive"); } }
}
