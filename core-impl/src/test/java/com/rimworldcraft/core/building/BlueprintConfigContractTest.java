package com.rimworldcraft.core.building;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BlueprintConfigContractTest {
    @Test void configuredBlueprintUsesPositiveDimensionsAndNonNegativeCosts(){Blueprint b=new Blueprint("rwc:wall","Wall","Basic wall",1,1,1,java.util.Map.of(com.rimworldcraft.core.api.types.ResourceType.WOOD,2),java.util.Map.of("0","minecraft:oak_planks"),20);assertThat(b.width()).isPositive();assertThat(b.requiredResources().values()).allMatch(v->v>=0);}
}
