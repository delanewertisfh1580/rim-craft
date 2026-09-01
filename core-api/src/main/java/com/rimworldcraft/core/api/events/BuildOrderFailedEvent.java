package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a build order failed. */
public final class BuildOrderFailedEvent extends DomainEvent { /** Creates the event. */ public BuildOrderFailedEvent(UUID sourceId) { super(sourceId); } }
