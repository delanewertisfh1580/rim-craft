package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.npc.domain.JobAssignment;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;

/** Use cases exposed by NPC Context. */
public interface NpcApplicationUseCases {
    void advanceNeeds(WorldId worldId, CitizenId citizenId, long elapsedTicks);
    void assignJob(WorldId worldId, CitizenId citizenId, JobAssignment job);
    void completeJob(WorldId worldId, CitizenId citizenId, long tick);
    void incapacitate(WorldId worldId, CitizenId citizenId);
    void recover(WorldId worldId, CitizenId citizenId);
    void die(WorldId worldId, CitizenId citizenId);
}
