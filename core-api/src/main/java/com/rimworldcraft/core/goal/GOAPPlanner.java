package com.rimworldcraft.core.goal;
import java.util.Optional;
public interface GOAPPlanner { Optional<Plan> plan(Goal goal,WorldState state); }
