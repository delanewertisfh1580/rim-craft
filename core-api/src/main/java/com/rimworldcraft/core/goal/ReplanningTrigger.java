package com.rimworldcraft.core.goal;
public interface ReplanningTrigger { boolean shouldReplan(WorldState previous,WorldState current,Plan plan); }
