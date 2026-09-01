package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an NPC relationship changed. */
public final class NPCRelationshipChangedEvent extends DomainEvent { /** Creates the event. */ public NPCRelationshipChangedEvent(UUID sourceId) { super(sourceId); } }
