package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an incident was resolved. */
public final class IncidentResolvedEvent extends DomainEvent { /** Creates the event. */ public IncidentResolvedEvent(UUID sourceId) { super(sourceId); } }
