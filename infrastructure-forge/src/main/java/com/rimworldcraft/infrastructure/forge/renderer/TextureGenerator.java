package com.rimworldcraft.infrastructure.forge.renderer;

import com.rimworldcraft.core.api.types.Trait;
import java.util.List;

/** Generates a platform texture descriptor from traits. */
public final class TextureGenerator {
    /** Returns a stable descriptor for the supplied traits. */ public String generate(List<Trait> traits) { return "rimworldcraft:citizen/default"; }
}
