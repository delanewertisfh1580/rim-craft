package com.rimworldcraft.core.test;

import com.rimworldcraft.core.api.ResourceType;
import com.rimworldcraft.test.TestDataFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** Unit tests for Colony; see module-colony.md. */
class ColonyTest {
    @Test void addResource_shouldIncreaseBalance() {
        var colony = TestDataFactory.colony();
        colony.addResource(ResourceType.WOOD, 3);
        assertThat(colony.resources()).containsEntry(ResourceType.WOOD, 3);
    }
    @Test void removeResource_shouldRejectInsufficientBalance() {
        var colony = TestDataFactory.colony();
        assertThatThrownBy(() -> colony.removeResource(ResourceType.WOOD, 1))
                .isInstanceOf(RuntimeException.class);
    }
}
