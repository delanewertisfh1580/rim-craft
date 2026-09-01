package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a task was assigned. */
public final class TaskAssignedEvent extends DomainEvent { /** Creates the event. */ public TaskAssignedEvent(UUID sourceId) { super(sourceId); } }
