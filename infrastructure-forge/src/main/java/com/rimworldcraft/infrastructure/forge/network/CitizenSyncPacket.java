package com.rimworldcraft.infrastructure.forge.network;

import com.rimworldcraft.core.api.types.Position;
import java.util.UUID;
/** Immutable citizen state packet. */
public record CitizenSyncPacket(UUID citizenId, Position position, int mood, String task) { }
