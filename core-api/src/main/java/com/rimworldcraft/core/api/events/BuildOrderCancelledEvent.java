package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a build order was cancelled. */
public final class BuildOrderCancelledEvent extends DomainEvent { /** Creates the event. */ public BuildOrderCancelledEvent(UUID sourceId) { super(sourceId); } }
