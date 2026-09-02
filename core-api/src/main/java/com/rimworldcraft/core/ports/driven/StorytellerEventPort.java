package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.storyteller.IncidentDecision;
import com.rimworldcraft.core.storyteller.IncidentOutcome;
import com.rimworldcraft.core.storyteller.SpawnEntryPointRequest;

/** Publishes immutable Storyteller facts to the event boundary. */
public interface StorytellerEventPort {
    void incidentScheduled(SpawnEntryPointRequest request);
    default void incidentPostponed(IncidentDecision decision) { }
    default void incidentOutcomeApplied(IncidentOutcome outcome) { }
}
