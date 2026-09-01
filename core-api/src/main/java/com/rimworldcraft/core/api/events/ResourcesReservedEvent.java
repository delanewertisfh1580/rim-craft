package com.rimworldcraft.core.api.events;
import java.util.UUID;
/** Signals that construction resources were reserved. */
public final class ResourcesReservedEvent extends DomainEvent { /** Creates the event. */ public ResourcesReservedEvent(UUID sourceId) { super(sourceId); } }
