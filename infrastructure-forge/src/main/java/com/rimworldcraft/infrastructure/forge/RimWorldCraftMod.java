package com.rimworldcraft.infrastructure.forge;

import com.rimworldcraft.infrastructure.common.adapter.JsonSaveAdapter;
import com.rimworldcraft.infrastructure.forge.adapter.ForgeEntityAdapter;
import com.rimworldcraft.infrastructure.forge.registry.*;
import java.nio.file.Path;

/** Forge composition root; bind this seam to @Mod during Forge-specific wiring. */
public final class RimWorldCraftMod {
    private final ModEntities entities = new ModEntities();
    private final ModRenderers renderers = new ModRenderers();
    private final JsonSaveAdapter saveAdapter;
    /** Creates the mod composition root. */
    public RimWorldCraftMod(Path saveDirectory) { this.saveAdapter = new JsonSaveAdapter(saveDirectory); }
    /** Initializes registries and core adapters. */
    public void onInitialize() { entities.registerDefaults(); renderers.registerDefaults(); }
    /** Handles a server tick. */ public void onServerTick() { }
    /** Handles a world save. */ public void onWorldSave() { saveAdapter.saveAll(); }
    /** Returns entity registry state. */ public ModEntities entities() { return entities; }
}
