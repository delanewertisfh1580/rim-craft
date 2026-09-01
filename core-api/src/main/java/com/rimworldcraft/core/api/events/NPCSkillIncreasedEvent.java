package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an NPC skill increased. */
public final class NPCSkillIncreasedEvent extends DomainEvent { /** Creates the event. */ public NPCSkillIncreasedEvent(UUID sourceId) { super(sourceId); } }
