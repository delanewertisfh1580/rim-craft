package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.ResourceType;
import java.util.Map;
import java.util.Objects;

/** Immutable, config-backed building definition. */
public record Blueprint(String id, String name, String description, int width, int height, int depth,
                        Map<ResourceType,Integer> requiredResources, Map<String,String> blocks, int estimatedTicks) {
    public Blueprint {
        requireText(id,"id"); requireText(name,"name"); requireText(description,"description"); Objects.requireNonNull(requiredResources); Objects.requireNonNull(blocks);
        if(width<=0||height<=0||depth<=0) throw new IllegalArgumentException("dimensions must be > 0");
        if(estimatedTicks<0) throw new IllegalArgumentException("estimatedTicks must be >= 0");
        requiredResources=Map.copyOf(requiredResources); blocks=Map.copyOf(blocks); validate();
    }
    public Blueprint(String id,String name,String description,int width,int height,int depth,Map<ResourceType,Integer> costs,int estimatedTicks){this(id,name,description,width,height,depth,costs,Map.of(),estimatedTicks);}
    public void validate(){if(requiredResources.values().stream().anyMatch(v->v==null||v<0))throw new IllegalArgumentException("costs must be >= 0");if(blocks.values().stream().anyMatch(v->v==null||v.isBlank()))throw new IllegalArgumentException("block type must not be blank");}
    private static void requireText(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" must not be blank");}
}
