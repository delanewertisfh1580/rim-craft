package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an NPC joined a colony. */
public final class NPCJoinedColonyEvent extends DomainEvent { /** Creates the event. */ public NPCJoinedColonyEvent(UUID sourceId) { super(sourceId); } }
