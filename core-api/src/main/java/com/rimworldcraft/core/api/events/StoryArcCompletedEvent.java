package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a story arc completed. */
public final class StoryArcCompletedEvent extends DomainEvent { /** Creates the event. */ public StoryArcCompletedEvent(UUID sourceId) { super(sourceId); } }
