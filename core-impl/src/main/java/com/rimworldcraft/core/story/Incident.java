package com.rimworldcraft.core.story;

import com.rimworldcraft.core.api.types.*;
import java.util.*;

/** Storyteller incident entity. */
public final class Incident {
    private final UUID id; private final IncidentType type; private final String description; private final int severity; private final int duration; private IncidentStatus status = IncidentStatus.PENDING; private IncidentOutcome outcome;
    /** Creates an incident. */
    public Incident(UUID id, IncidentType type, String description, int severity, int duration) { this.id = Objects.requireNonNull(id); this.type = Objects.requireNonNull(type); this.description = Objects.requireNonNull(description); if (severity < 0 || duration < 0) throw new IllegalArgumentException("Invalid incident"); this.severity = severity; this.duration = duration; }
    /** Returns incident identity. */ public UUID getId() { return id; }
    /** Returns incident type. */ public IncidentType getType() { return type; }
    /** Returns lifecycle status. */ public IncidentStatus getStatus() { return status; }
    /** Returns outcome when resolved. */ public IncidentOutcome getOutcome() { return outcome; }
    /** Starts a pending incident. */ public void start() { if (status != IncidentStatus.PENDING) throw new IllegalStateException("Incident already started"); status = IncidentStatus.ACTIVE; }
    /** Resolves an active incident. */ public void resolve(IncidentOutcome result) { if (status != IncidentStatus.ACTIVE) throw new IllegalStateException("Incident is not active"); outcome = Objects.requireNonNull(result); status = result == IncidentOutcome.FAILURE ? IncidentStatus.FAILED : IncidentStatus.RESOLVED; }
    /** Marks an incident failed. */ public void fail() { status = IncidentStatus.FAILED; outcome = IncidentOutcome.FAILURE; }
}
