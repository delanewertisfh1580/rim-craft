package com.rimworldcraft.core.player;

import com.rimworldcraft.core.shared.CommandId;
import java.util.Objects;

/** Result of a server-side Player command decision. */
public record PlayerCommandResult(CommandId commandId, Status status, String reason) {
    public PlayerCommandResult {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(status, "status");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason must not be blank");
    }

    public boolean accepted() { return status == Status.ACCEPTED || status == Status.REPLAYED_ACCEPTED; }
    public boolean replay() { return status == Status.REPLAYED_ACCEPTED || status == Status.REPLAYED_REJECTED; }

    public enum Status { ACCEPTED, REJECTED, REPLAYED_ACCEPTED, REPLAYED_REJECTED }
}
