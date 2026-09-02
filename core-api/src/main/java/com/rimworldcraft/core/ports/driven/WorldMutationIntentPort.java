package com.rimworldcraft.core.ports.driven;
import com.rimworldcraft.core.building.BuildOrder;
public interface WorldMutationIntentPort { void submitPlacement(BuildOrder order); void submitRemoval(BuildOrder order); }
