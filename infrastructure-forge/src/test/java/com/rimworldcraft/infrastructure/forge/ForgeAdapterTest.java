package com.rimworldcraft.infrastructure.forge;

import com.rimworldcraft.core.api.types.Position;
import com.rimworldcraft.infrastructure.forge.adapter.ForgeEntityAdapter;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** JVM tests for platform adapter seams before Forge runtime wiring. */
class ForgeAdapterTest {
    @Test void entityAdapter_shouldTrackSpawnPosition() {
        var adapter = new ForgeEntityAdapter(new Object());
        UUID id = UUID.randomUUID(); Position position = new Position(1, 64, 2);
        adapter.spawnCitizen(id, position);
        assertThat(adapter.getEntityPosition(id)).contains(position);
    }
}
