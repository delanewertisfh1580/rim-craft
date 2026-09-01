package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that the storyteller processed a tick. */
public final class StorytellerTickedEvent extends DomainEvent { /** Creates the event. */ public StorytellerTickedEvent(UUID sourceId) { super(sourceId); } }
