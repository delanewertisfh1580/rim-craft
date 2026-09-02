package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.npc.domain.JobAssignment;
import com.rimworldcraft.core.shared.CitizenId;

/** Emits platform-neutral execution intents; never invokes Minecraft directly. */
public interface JobExecutionIntentPort {
    void submit(CitizenId citizenId, JobAssignment assignment);
}
