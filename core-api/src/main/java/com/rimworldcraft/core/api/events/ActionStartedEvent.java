package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that an AI action started. */
public final class ActionStartedEvent extends DomainEvent { /** Creates the event. */ public ActionStartedEvent(UUID sourceId) { super(sourceId); } }
