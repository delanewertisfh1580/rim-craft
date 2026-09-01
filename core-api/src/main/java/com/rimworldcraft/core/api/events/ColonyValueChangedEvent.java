package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that colony value changed. */
public final class ColonyValueChangedEvent extends DomainEvent {
    private final long oldValue;
    private final long newValue;
    /** Creates the event. */
    public ColonyValueChangedEvent(UUID sourceId, long oldValue, long newValue) { super(sourceId); this.oldValue = oldValue; this.newValue = newValue; }
    /** Returns the previous value. */
    public long getOldValue() { return oldValue; }
    /** Returns the new value. */
    public long getNewValue() { return newValue; }
}
