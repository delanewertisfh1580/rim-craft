package com.rimworldcraft.core.npc.application;

import com.rimworldcraft.core.npc.domain.Citizen;
import com.rimworldcraft.core.npc.domain.SkillType;

/** Applies validated experience gains. */
public interface SkillExperiencePolicy { void award(Citizen citizen, SkillType type, int amount); }
