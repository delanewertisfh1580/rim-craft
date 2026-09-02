package com.rimworldcraft.core.npc.domain;

import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.Optional;

/** Aggregate root owning individual NPC state and lifecycle. */
public final class Citizen {
    private final CitizenId id; private final WorldId worldId; private final String name; private final ColonyId colonyId;
    private NeedState needs; private MoodState mood; private HealthState health; private TraitSet traits; private SkillSet skills;
    private Schedule schedule; private CitizenStatus status; private JobAssignment assignment;

    private Citizen(CitizenId id, WorldId worldId, String name, ColonyId colonyId, NeedState needs, MoodState mood,
                    HealthState health, TraitSet traits, SkillSet skills, Schedule schedule) {
        this.id=Objects.requireNonNull(id,"id"); this.worldId=Objects.requireNonNull(worldId,"worldId");
        this.name=requireText(name); this.colonyId=colonyId; this.needs=Objects.requireNonNull(needs,"needs");
        this.mood=Objects.requireNonNull(mood,"mood"); this.health=Objects.requireNonNull(health,"health");
        this.traits=Objects.requireNonNull(traits,"traits"); this.skills=Objects.requireNonNull(skills,"skills");
        this.schedule=Objects.requireNonNull(schedule,"schedule"); this.status=CitizenStatus.ACTIVE;
    }
    public static Citizen create(CitizenId id, WorldId worldId, String name, ColonyId colonyId, NeedState needs,
                                 MoodState mood, HealthState health, TraitSet traits, SkillSet skills, Schedule schedule) {
        return new Citizen(id, worldId, name, colonyId, needs, mood, health, traits, skills, schedule);
    }
    public CitizenId id(){return id;} public WorldId worldId(){return worldId;} public String name(){return name;}
    public Optional<ColonyId> colonyId(){return Optional.ofNullable(colonyId);} public NeedState needs(){return needs;}
    public MoodState mood(){return mood;} public HealthState health(){return health;} public TraitSet traits(){return traits;}
    public SkillSet skills(){return skills;} public Schedule schedule(){return schedule;} public CitizenStatus status(){return status;}
    public Optional<JobAssignment> assignment(){return Optional.ofNullable(assignment);}
    public void decayNeeds(long ticks) { requireNotDead(); needs=needs.decay(ticks); }
    public void changeMood(int delta, long tick) { requireNotDead(); mood=mood.adjust(delta, tick); }
    public void gainSkill(SkillType type, int amount) { requireNotDead(); skills=skills.gain(type, amount); }
    public void assignJob(JobAssignment job) { requireNotDead(); if(status==CitizenStatus.INCAPACITATED) throw new IllegalStateException("incapacitated citizen cannot accept jobs"); if(!job.worldId().equals(worldId)) throw new IllegalArgumentException("world mismatch"); assignment=Objects.requireNonNull(job,"job"); }
    public void completeJob(long tick) { requireNotDead(); if(assignment==null) throw new IllegalStateException("no job assigned"); assignment=null; }
    public void incapacitate() { if(status!=CitizenStatus.DEAD) status=CitizenStatus.INCAPACITATED; }
    public void recover() { if(status==CitizenStatus.INCAPACITATED) status=CitizenStatus.ACTIVE; }
    public void die() { if(status!=CitizenStatus.DEAD) { status=CitizenStatus.DEAD; assignment=null; } }
    private void requireNotDead(){if(status==CitizenStatus.DEAD) throw new IllegalStateException("dead citizen is terminal");}
    private static String requireText(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("name must not be blank");return value;}
}
