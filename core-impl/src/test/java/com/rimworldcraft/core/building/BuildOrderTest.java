package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Unit tests for BuildOrder aggregate. */
class BuildOrderTest {
    private BuildOrder order(){return new BuildOrder(UUID.randomUUID(),UUID.randomUUID(),new Blueprint("wall","Wall","wall",1,1,1,Map.of(ResourceType.WOOD,2),0),new GridPosition(0,64,0),1);}
    @Test void progress_shouldReachCompletion(){BuildOrder order=order();order.updateProgress(100);assertThat(order.getProgress()).isEqualTo(100);order.complete();assertThat(order.getStatus()).isEqualTo(BuildOrderStatus.COMPLETED);}
    @Test void complete_shouldRejectIncompleteOrder(){assertThatThrownBy(()->order().complete()).isInstanceOf(IllegalStateException.class);}
}
