package com.rimworldcraft.integration.test;

import com.rimworldcraft.core.api.*;
import com.rimworldcraft.infrastructure.forge.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** JVM integration smoke tests; replace with TestModLoader fixtures when platform wiring is added. */
@Tag("integration")
class AdapterIntegrationTest {
    @Test void saveLoad_shouldRoundTripColonyId() {
        var adapter = new NbtSaveAdapter();
        var id = UUID.randomUUID();
        adapter.saveColony(id);
        assertThat(adapter.loadColony(id)).contains(id);
    }
    @Test void pathfinder_shouldReturnEmptyForUnknownRoute() {
        var adapter = new BaritonePathfinderAdapter();
        assertThat(adapter.findPath(new Position(0, 64, 0), new Position(10, 64, 10), EntityTraversalContext.walking())).isEmpty();
    }
}
