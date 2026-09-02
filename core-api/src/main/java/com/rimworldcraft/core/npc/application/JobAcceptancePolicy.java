package com.rimworldcraft.core.npc.application;

import com.rimworldcraft.core.npc.domain.Citizen;
import com.rimworldcraft.core.npc.domain.JobAssignment;

/** Decides whether a citizen can accept a normal job. */
public interface JobAcceptancePolicy { boolean accepts(Citizen citizen, JobAssignment job); }
