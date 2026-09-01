package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a build order completed. */
public final class BuildOrderCompletedEvent extends DomainEvent { /** Creates the event. */ public BuildOrderCompletedEvent(UUID sourceId) { super(sourceId); } }
