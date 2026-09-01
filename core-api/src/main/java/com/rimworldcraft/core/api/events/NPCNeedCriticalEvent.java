package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an NPC need reached a critical threshold. */
public final class NPCNeedCriticalEvent extends DomainEvent { /** Creates the event. */ public NPCNeedCriticalEvent(UUID sourceId) { super(sourceId); } }
