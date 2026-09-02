package com.rimworldcraft.core.npc.application;

import com.rimworldcraft.core.contracts.NpcJobCompletedEvent;
import com.rimworldcraft.core.npc.domain.Citizen;
import com.rimworldcraft.core.npc.domain.JobAssignment;
import com.rimworldcraft.core.npc.domain.JobAttempt;
import com.rimworldcraft.core.ports.driven.CitizenRepository;
import com.rimworldcraft.core.ports.driven.JobExecutionIntentPort;
import com.rimworldcraft.core.ports.driven.NpcEventPublicationPort;
import com.rimworldcraft.core.ports.driving.NpcApplicationUseCases;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

/** Application orchestration for the platform-neutral NPC context. */
public final class DefaultNpcApplicationService implements NpcApplicationUseCases {
    private final CitizenRepository citizens; private final NeedDecayPolicy decay; private final JobAcceptancePolicy acceptance;
    private final JobExecutionIntentPort execution; private final NpcEventPublicationPort events;
    public DefaultNpcApplicationService(CitizenRepository citizens, NeedDecayPolicy decay, JobAcceptancePolicy acceptance,
                                        JobExecutionIntentPort execution, NpcEventPublicationPort events) {
        this.citizens=Objects.requireNonNull(citizens); this.decay=Objects.requireNonNull(decay); this.acceptance=Objects.requireNonNull(acceptance);
        this.execution=Objects.requireNonNull(execution); this.events=Objects.requireNonNull(events);
    }
    @Override public void advanceNeeds(WorldId worldId, CitizenId id, long ticks) { Citizen c=load(worldId,id); decay.apply(c,ticks); citizens.save(c); }
    @Override public void assignJob(WorldId worldId, CitizenId id, JobAssignment job) { Citizen c=load(worldId,id); if(!acceptance.accepts(c,job)) throw new IllegalStateException("job rejected"); c.assignJob(job); citizens.save(c); execution.submit(id,job); }
    @Override public void completeJob(WorldId worldId, CitizenId id, long tick) { Citizen c=load(worldId,id); JobAssignment job=c.assignment().orElseThrow(() -> new IllegalStateException("no job assigned")); c.completeJob(tick); citizens.save(c); events.publishJobCompleted(new NpcJobCompletedEvent(worldId,id,job.jobType(),new JobAttempt(job.jobId(),JobAttempt.Status.COMPLETED,tick,""))); }
    @Override public void incapacitate(WorldId w, CitizenId id){Citizen c=load(w,id);c.incapacitate();citizens.save(c);}
    @Override public void recover(WorldId w, CitizenId id){Citizen c=load(w,id);c.recover();citizens.save(c);}
    @Override public void die(WorldId w, CitizenId id){Citizen c=load(w,id);c.die();citizens.save(c);}
    private Citizen load(WorldId worldId,CitizenId id){return citizens.find(Objects.requireNonNull(worldId),Objects.requireNonNull(id)).orElseThrow(() -> new IllegalArgumentException("citizen not found"));}
}
