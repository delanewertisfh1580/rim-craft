package com.rimworldcraft.core.npc.application;

import com.rimworldcraft.core.npc.domain.Citizen;

/** Calculates mood from current needs and traits. */
public interface MoodPolicy { int moodDelta(Citizen citizen); }
