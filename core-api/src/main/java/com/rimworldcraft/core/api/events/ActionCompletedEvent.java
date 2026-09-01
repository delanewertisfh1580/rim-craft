package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an AI action completed. */
public final class ActionCompletedEvent extends DomainEvent { /** Creates the event. */ public ActionCompletedEvent(UUID sourceId) { super(sourceId); } }
