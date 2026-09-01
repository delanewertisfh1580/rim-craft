package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an NPC mood changed. */
public final class NPCMoodChangedEvent extends DomainEvent { /** Creates the event. */ public NPCMoodChangedEvent(UUID sourceId) { super(sourceId); } }
