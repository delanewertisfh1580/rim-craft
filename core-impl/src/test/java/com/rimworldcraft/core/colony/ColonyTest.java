package com.rimworldcraft.core.colony;

import com.rimworldcraft.core.api.types.ResourceType;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Unit tests for Colony aggregate. */
class ColonyTest {
    @Test void addResource_shouldIncreaseBalance() { Colony colony=new Colony(UUID.randomUUID(),"Home"); colony.addResource(ResourceType.WOOD,4); assertThat(colony.getResources()).containsEntry(ResourceType.WOOD,4); }
    @Test void removeResource_shouldRejectInsufficientAmount() { Colony colony=new Colony(UUID.randomUUID(),"Home"); assertThatThrownBy(()->colony.removeResource(ResourceType.WOOD,1)).isInstanceOf(RuntimeException.class); }
    @Test void recalculateValue_shouldIncludeCitizens() { Colony colony=new Colony(UUID.randomUUID(),"Home"); colony.addCitizen(UUID.randomUUID()); assertThat(colony.recalculateValue()).isEqualTo(50); }
}
