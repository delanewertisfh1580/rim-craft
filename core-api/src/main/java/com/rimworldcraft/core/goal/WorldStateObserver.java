package com.rimworldcraft.core.goal;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
public interface WorldStateObserver { WorldState observe(WorldId worldId,CitizenId citizenId,long tick); }
