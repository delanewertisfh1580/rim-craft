package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a story arc progressed. */
public final class StoryArcProgressedEvent extends DomainEvent { /** Creates the event. */ public StoryArcProgressedEvent(UUID sourceId) { super(sourceId); } }
