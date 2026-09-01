package com.rimworldcraft.infrastructure.forge.registry;

import com.rimworldcraft.infrastructure.forge.renderer.CitizenRenderer;
/** Renderer registration seam for Forge client setup. */
public final class ModRenderers {
    /** Registers the citizen renderer. */ public void registerDefaults(){new CitizenRenderer();}
}
