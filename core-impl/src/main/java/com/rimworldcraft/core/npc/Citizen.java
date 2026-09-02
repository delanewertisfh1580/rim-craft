package com.rimworldcraft.core.npc;

import com.rimworldcraft.core.api.events.*;
import com.rimworldcraft.core.api.types.Gender;
import com.rimworldcraft.core.api.types.*;
import java.util.*;

/** Citizen aggregate root; see module-npc-core.md. */
public final class Citizen {
    private final UUID citizenId; private final String name; private final Gender gender; private final String race; private final Date birthDate; private final List<String> traitIds; private final Map<SkillType, Skill> skills = new EnumMap<>(SkillType.class); private final Map<NeedType, Integer> needs = new EnumMap<>(NeedType.class); private final Map<UUID, Integer> relationships = new HashMap<>(); private final UUID colonyId; private Mood mood = new Mood(50, List.of()); private boolean alive = true; private String currentTask; private Position position;
    /** Creates a citizen with default needs and skills. */ public Citizen(UUID citizenId, String name, Gender gender, String race, UUID colonyId, Position position) { this.citizenId=Objects.requireNonNull(citizenId); this.name=requireText(name); this.gender=Objects.requireNonNull(gender); this.race=requireText(race); this.colonyId=Objects.requireNonNull(colonyId); this.position=Objects.requireNonNull(position); this.birthDate=new Date(); this.traitIds=new ArrayList<>(); for(NeedType type:NeedType.values()) needs.put(type,100); for(SkillType type:SkillType.values()) skills.put(type,new Skill(0,0)); }
    /** Returns citizen identifier. */ public UUID getCitizenId(){return citizenId;} /** Returns citizen name. */ public String getName(){return name;} /** Returns mood. */ public Mood getMood(){return mood;} /** Returns position. */ public Position getPosition(){return position;} /** Returns alive state. */ public boolean isAlive(){return alive;} /** Returns needs snapshot. */ public Map<NeedType,Integer> getNeeds(){return Map.copyOf(needs);} /** Returns skills snapshot. */ public Map<SkillType,Skill> getSkills(){return Map.copyOf(skills);}
    /** Updates needs and emits critical events. */ public List<DomainEvent> updateNeeds(long ticks){if(ticks<0) throw new IllegalArgumentException("ticks"); List<DomainEvent> events=new ArrayList<>(); int decay=(int)Math.min(Integer.MAX_VALUE,ticks/100); for(NeedType type:NeedType.values()){int value=Math.max(0,needs.get(type)-decay); needs.put(type,value); if(value<20) events.add(new NPCNeedCriticalEvent(citizenId));} return List.copyOf(events);}
    /** Changes mood and emits an event. */ public List<DomainEvent> changeMood(int delta,String reason){mood=new Mood(Math.max(0,Math.min(100,mood.value()+delta)),List.of(reason==null?"unknown":reason)); return List.of(new NPCMoodChangedEvent(citizenId));}
    /** Adds skill experience and emits an event. */ public List<DomainEvent> addSkillExperience(SkillType type,int experience){Objects.requireNonNull(type); if(experience<0) throw new IllegalArgumentException("experience"); skills.put(type,skills.get(type).addExperience(experience)); return List.of(new NPCSkillIncreasedEvent(citizenId));}
    /** Modifies a relationship score. */ public List<DomainEvent> modifyRelationship(UUID targetId,int delta){Objects.requireNonNull(targetId); relationships.put(targetId,Math.max(-100,Math.min(100,relationships.getOrDefault(targetId,0)+delta))); return List.of(new NPCRelationshipChangedEvent(citizenId));}
    /** Assigns a current task. */ public void assignTask(String task){currentTask=task;}
    /** Marks the citizen dead. */ public List<DomainEvent> die(String cause){if(!alive)return List.of(); alive=false; currentTask=null; return List.of(new NPCDeathEvent(citizenId));}
    /** Moves the projected citizen. */ public void moveTo(Position next){position=Objects.requireNonNull(next);}
    private static String requireText(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("text");return value;}
}
