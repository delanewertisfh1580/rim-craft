package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BuildingSystemTest {
    private final Blueprint blueprint=new Blueprint("wall","Wall","wall",2,2,2,Map.of(ResourceType.WOOD,3),Map.of("0","minecraft:oak"),10);
    private BuildOrder order(){return new BuildOrder(UUID.randomUUID(),UUID.randomUUID(),blueprint,new GridPosition(0,0,0),1);}
    @Test void dimensionsAndCostsAreValidated(){assertThatThrownBy(()->new Blueprint("x","x","x",0,1,1,Map.of(),Map.of(),0)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new Blueprint("x","x","x",1,1,1,Map.of(ResourceType.WOOD,-1),Map.of(),0)).isInstanceOf(IllegalArgumentException.class);}
    @Test void lifecycleInvariantsHold(){BuildOrder o=order();o.reserve();o.assignCitizen(UUID.randomUUID());o.updateProgress(100);o.complete();assertThat(o.status()).isEqualTo(BuildOrderStatus.COMPLETED);assertThatThrownBy(o::cancel).isInstanceOf(IllegalStateException.class);}
    @Test void failedOrderCannotProgress(){BuildOrder o=order();o.fail();assertThatThrownBy(()->o.updateProgress(1)).isInstanceOf(IllegalStateException.class);}
    @Test void resourceResultsAreIdempotent(){BuildOrder o=order();UUID result=UUID.randomUUID();assertThat(o.applyResourceResult(result,ResourceType.WOOD,2)).isTrue();assertThat(o.applyResourceResult(result,ResourceType.WOOD,2)).isFalse();assertThat(o.consumed()).containsEntry(ResourceType.WOOD,2);}
    @Test void ghostBlockRejectsInvalidType(){assertThatThrownBy(()->new GhostBlock(new GridPosition(0,0,0),"",0,UUID.randomUUID(),UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);}
}
