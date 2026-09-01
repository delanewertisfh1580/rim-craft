package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a goal changed. */
public final class GoalChangedEvent extends DomainEvent { /** Creates the event. */ public GoalChangedEvent(UUID sourceId) { super(sourceId); } }
