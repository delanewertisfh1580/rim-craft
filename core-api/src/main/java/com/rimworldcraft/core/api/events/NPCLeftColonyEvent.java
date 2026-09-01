package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an NPC left a colony. */
public final class NPCLeftColonyEvent extends DomainEvent { /** Creates the event. */ public NPCLeftColonyEvent(UUID sourceId) { super(sourceId); } }
