package com.rimworldcraft.core.npc.application;

import com.rimworldcraft.core.npc.domain.Citizen;
import com.rimworldcraft.core.npc.domain.SkillType;

/** Reads immutable trait effects for a specific skill or mood calculation. */
public interface TraitModifierPolicy { int moodModifier(Citizen citizen); int skillModifier(Citizen citizen, SkillType type); }
