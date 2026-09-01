package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that build progress changed. */
public final class BuildOrderProgressedEvent extends DomainEvent { /** Creates the event. */ public BuildOrderProgressedEvent(UUID sourceId) { super(sourceId); } }
