package com.rimworldcraft.core.goal;

import java.util.UUID;

/** Emits navigation intents; an adapter performs pathfinding and movement. */
public interface PathfindingIntentPort {
    void request(PathRequest request);
    void cancel(UUID requestId);
}
