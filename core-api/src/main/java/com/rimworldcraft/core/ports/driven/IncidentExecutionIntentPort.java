package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.storyteller.SpawnEntryPointRequest;

/** Materializes storyteller decisions outside Core; it never creates platform entities itself. */
public interface IncidentExecutionIntentPort {
    void submit(SpawnEntryPointRequest request);
    default void cancel(SpawnEntryPointRequest request) { }
}
