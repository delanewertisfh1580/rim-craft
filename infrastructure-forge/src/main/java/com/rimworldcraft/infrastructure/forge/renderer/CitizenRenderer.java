package com.rimworldcraft.infrastructure.forge.renderer;

import com.rimworldcraft.infrastructure.forge.entity.EntityCitizen;
import java.util.Objects;

/** Client rendering seam; platform renderer inheritance is added with Forge mappings. */
public final class CitizenRenderer {
    /** Creates a renderer with no domain dependencies. */ public CitizenRenderer() { }
    /** Renders a citizen using platform-specific code in the future. */ public void render(EntityCitizen entity) { Objects.requireNonNull(entity); }
}
