package com.rimworldcraft.core.story;

import com.rimworldcraft.core.api.events.*;
import java.util.*;
/** Storyteller aggregate root. */
public final class Storyteller {
    private final UUID colonyId; private final List<HistoryEntry> history=new ArrayList<>(); private final List<Incident> activeIncidents=new ArrayList<>(); private final Map<String,Integer> storyArcProgress=new HashMap<>(); private long lastEventTimestamp; private final StorytellerConfig config;
    /** Creates a storyteller. */ public Storyteller(UUID colonyId,StorytellerConfig config){this.colonyId=Objects.requireNonNull(colonyId);this.config=Objects.requireNonNull(config);}
    /** Returns colony ID. */ public UUID getColonyId(){return colonyId;} /** Returns active incidents. */ public List<Incident> getActiveIncidents(){return List.copyOf(activeIncidents);}
    /** Starts an incident if capacity allows. */ public List<DomainEvent> generateIncident(Incident incident){Objects.requireNonNull(incident);if(activeIncidents.size()>=config.maxActiveIncidents())return List.of();incident.start();activeIncidents.add(incident);return List.of(new IncidentStartedEvent(incident.getId()));}
    /** Resolves an active incident. */ public List<DomainEvent> resolveIncident(UUID id,IncidentOutcome outcome){Incident incident=activeIncidents.stream().filter(item->item.getId().equals(id)).findFirst().orElseThrow();incident.resolve(outcome);activeIncidents.remove(incident);return List.of(outcome==IncidentOutcome.FAILURE?new IncidentFailedEvent(id):new IncidentResolvedEvent(id));}
    /** Adds history. */ public void addHistoryEntry(HistoryEntry entry){history.add(Objects.requireNonNull(entry));}
    /** Advances storyteller time. */ public List<DomainEvent> tick(long currentTime){if(currentTime<lastEventTimestamp)throw new IllegalArgumentException("time");lastEventTimestamp=currentTime;return List.of(new StorytellerTickedEvent(colonyId));}
}
