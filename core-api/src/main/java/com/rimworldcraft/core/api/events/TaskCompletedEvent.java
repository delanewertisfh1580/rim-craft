package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a task completed. */
public final class TaskCompletedEvent extends DomainEvent { /** Creates the event. */ public TaskCompletedEvent(UUID sourceId) { super(sourceId); } }
