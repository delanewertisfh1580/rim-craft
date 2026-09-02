package com.rimworldcraft.core.ports.driven;
import com.rimworldcraft.core.goal.CitizenAIState;
import com.rimworldcraft.core.shared.CitizenId;
import java.util.Optional;
public interface CitizenAIRepository { Optional<CitizenAIState> find(CitizenId id); void save(CitizenAIState state); }
