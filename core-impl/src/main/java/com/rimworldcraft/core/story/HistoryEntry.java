package com.rimworldcraft.core.story;

import java.time.Instant;
import java.util.UUID;
/** Immutable storyteller history entry. */
public record HistoryEntry(Instant timestamp, UUID incidentId, String shortDescription, int influence) { }
