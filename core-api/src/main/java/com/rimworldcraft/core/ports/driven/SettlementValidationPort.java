package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.api.types.WorldId;

/** Validates settlement sites without exposing platform types to Core. */
public interface SettlementValidationPort {
    /** Returns whether the site is valid in the specified world. */
    boolean isValid(WorldId worldId, GridPosition position);
}
