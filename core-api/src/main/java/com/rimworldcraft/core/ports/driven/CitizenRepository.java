package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.npc.domain.Citizen;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Optional;

/** Persistence boundary for Citizen aggregates. */
public interface CitizenRepository { Optional<Citizen> find(WorldId worldId, CitizenId citizenId); void save(Citizen citizen); }
