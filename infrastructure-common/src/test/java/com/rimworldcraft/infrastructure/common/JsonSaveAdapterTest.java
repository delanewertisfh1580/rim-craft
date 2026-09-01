package com.rimworldcraft.infrastructure.common;

import com.rimworldcraft.infrastructure.common.adapter.JsonSaveAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Tests JSON save lifecycle behavior. */
class JsonSaveAdapterTest {
    @TempDir Path tempDirectory;
    @Test void saveColony_shouldCreateColonyDocument() {
        JsonSaveAdapter adapter = new JsonSaveAdapter(tempDirectory);
        UUID colonyId = UUID.randomUUID();
        adapter.saveColony(colonyId);
        assertThat(adapter.exists(colonyId)).isTrue();
        assertThat(tempDirectory.resolve("colonies").resolve(colonyId.toString()).resolve("colony.json")).exists();
    }
    @Test void deleteColony_shouldRemoveSave() {
        JsonSaveAdapter adapter = new JsonSaveAdapter(tempDirectory);
        UUID colonyId = UUID.randomUUID(); adapter.saveColony(colonyId); adapter.deleteColony(colonyId);
        assertThat(adapter.exists(colonyId)).isFalse();
    }
}
