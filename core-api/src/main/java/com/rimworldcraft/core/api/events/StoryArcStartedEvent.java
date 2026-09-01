package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a story arc started. */
public final class StoryArcStartedEvent extends DomainEvent { /** Creates the event. */ public StoryArcStartedEvent(UUID sourceId) { super(sourceId); } }
