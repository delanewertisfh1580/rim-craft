package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an AI plan failed. */
public final class PlanFailedEvent extends DomainEvent { /** Creates the event. */ public PlanFailedEvent(UUID sourceId) { super(sourceId); } }
