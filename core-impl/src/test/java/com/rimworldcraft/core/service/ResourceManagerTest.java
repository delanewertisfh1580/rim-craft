package com.rimworldcraft.core.service;

import com.rimworldcraft.core.api.types.ResourceType;
import com.rimworldcraft.core.colony.Colony;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Unit tests for ResourceManager. */
class ResourceManagerTest {
    @Test void reserveResources_shouldMoveAvailableItems(){Colony colony=new Colony(UUID.randomUUID(),"Home");colony.addResource(ResourceType.WOOD,5);assertThat(new ResourceManager().reserveResources(colony,Map.of(ResourceType.WOOD,3))).isTrue();assertThat(colony.getResources()).containsEntry(ResourceType.WOOD,2);}
    @Test void reserveResources_shouldReturnFalseWhenMissing(){Colony colony=new Colony(UUID.randomUUID(),"Home");assertThat(new ResourceManager().reserveResources(colony,Map.of(ResourceType.WOOD,3))).isFalse();}
}
