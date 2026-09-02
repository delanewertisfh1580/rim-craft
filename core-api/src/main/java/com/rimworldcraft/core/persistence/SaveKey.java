package com.rimworldcraft.core.persistence;

import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

public record SaveKey(WorldId worldId, String aggregateType, String aggregateId) {
    public SaveKey {
        Objects.requireNonNull(worldId); Objects.requireNonNull(aggregateType); Objects.requireNonNull(aggregateId);
        if (!aggregateType.matches("[a-z][a-z0-9_-]{1,63}")) throw new IllegalArgumentException("invalid aggregate type");
        if (!aggregateId.matches("[a-zA-Z0-9._:-]{1,128}") || aggregateId.contains("..")) throw new IllegalArgumentException("invalid aggregate id");
    }
    public String logicalPath() { return "rwc/" + worldId.value() + "/" + aggregateType + "/" + aggregateId; }
}
