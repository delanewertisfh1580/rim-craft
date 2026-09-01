package com.rimworldcraft.infrastructure.forge.adapter;

import com.rimworldcraft.core.api.ports.ISaveLoadPort;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/** Forge save facade; replace the delegate with CompoundTag persistence in platform wiring. */
public final class ForgeSaveAdapter implements ISaveLoadPort {
    private final ISaveLoadPort delegate;
    /** Creates a Forge save facade. */ public ForgeSaveAdapter(ISaveLoadPort delegate) { this.delegate = Objects.requireNonNull(delegate); }
    /** Saves one colony. */ public void saveColony(UUID colonyId) { delegate.saveColony(colonyId); }
    /** Loads one colony. */ public void loadColony(UUID colonyId) { delegate.loadColony(colonyId); }
    /** Checks save existence. */ public boolean exists(UUID colonyId) { return delegate.exists(colonyId); }
    /** Deletes one colony. */ public void deleteColony(UUID colonyId) { delegate.deleteColony(colonyId); }
    /** Saves all state. */ public void saveAll() { delegate.saveAll(); }
    /** Loads all state. */ public void loadAll() { delegate.loadAll(); }
}
