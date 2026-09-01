package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a build order was assigned. */
public final class BuildOrderAssignedEvent extends DomainEvent { /** Creates the event. */ public BuildOrderAssignedEvent(UUID sourceId) { super(sourceId); } }
