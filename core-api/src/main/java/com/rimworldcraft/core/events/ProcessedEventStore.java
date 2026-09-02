package com.rimworldcraft.core.events;

public interface ProcessedEventStore { boolean alreadyProcessed(String handlerId, java.util.UUID eventId); void markProcessed(String handlerId, java.util.UUID eventId); }
