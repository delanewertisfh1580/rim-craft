package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an incident failed. */
public final class IncidentFailedEvent extends DomainEvent { /** Creates the event. */ public IncidentFailedEvent(UUID sourceId) { super(sourceId); } }
