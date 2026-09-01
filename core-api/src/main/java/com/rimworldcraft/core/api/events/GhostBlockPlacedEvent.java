package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that a ghost block was placed. */
public final class GhostBlockPlacedEvent extends DomainEvent { /** Creates the event. */ public GhostBlockPlacedEvent(UUID sourceId) { super(sourceId); } }
