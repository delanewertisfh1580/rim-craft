package com.rimworldcraft.core.npc.application;

import com.rimworldcraft.core.npc.domain.Citizen;

/** Applies configured tick-based decay without consulting wall-clock time. */
public interface NeedDecayPolicy { void apply(Citizen citizen, long elapsedTicks); }
