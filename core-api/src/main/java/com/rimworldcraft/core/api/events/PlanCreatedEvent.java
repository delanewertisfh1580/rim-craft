package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an AI plan was created. */
public final class PlanCreatedEvent extends DomainEvent { /** Creates the event. */ public PlanCreatedEvent(UUID sourceId) { super(sourceId); } }
