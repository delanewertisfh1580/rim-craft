package com.rimworldcraft.infrastructure.forge.network;

import java.util.UUID;
/** Immutable client interaction request. */
public record CitizenInteractionPacket(UUID citizenId, InteractionType interaction) {
    /** Supported interaction types. */ public enum InteractionType { USE, ATTACK }
}
