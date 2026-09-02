package com.rimworldcraft.core.player;

import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import java.util.Objects;
import java.util.Optional;

/** Player UI selection projection; it never stores an aggregate copy. */
public record PlayerSelection(ColonyId colonyId, CitizenId citizenId) {
    public PlayerSelection {
        Objects.requireNonNull(colonyId, "colonyId");
    }

    public static PlayerSelection forColony(ColonyId colonyId) {
        return new PlayerSelection(colonyId, null);
    }

    public Optional<CitizenId> selectedCitizen() {
        return Optional.ofNullable(citizenId);
    }
}
