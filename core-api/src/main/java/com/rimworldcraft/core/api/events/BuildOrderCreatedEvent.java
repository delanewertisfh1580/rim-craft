package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a build order was created. */
public final class BuildOrderCreatedEvent extends DomainEvent { /** Creates the event. */ public BuildOrderCreatedEvent(UUID sourceId) { super(sourceId); } }
