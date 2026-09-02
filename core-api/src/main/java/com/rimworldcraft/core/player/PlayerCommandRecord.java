package com.rimworldcraft.core.player;

import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.GameTick;
import java.util.Objects;

/** Immutable receipt used to reject command replays and preserve the original result. */
public record PlayerCommandRecord(CommandId commandId, String operation, CommandStatus status,
                                  String reason, GameTick processedAt) {
    public PlayerCommandRecord {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(processedAt, "processedAt");
        if (operation == null || operation.isBlank()) throw new IllegalArgumentException("operation must not be blank");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason must not be blank");
        operation = operation.trim();
        reason = reason.trim();
    }

    public enum CommandStatus { ACCEPTED, REJECTED }
}
