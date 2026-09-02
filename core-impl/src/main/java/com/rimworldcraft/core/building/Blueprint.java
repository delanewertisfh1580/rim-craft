package com.rimworldcraft.core.building;
import com.rimworldcraft.core.api.types.ResourceType;
import java.util.Map;
import java.util.Objects;
public record Blueprint(String id,String name,String description,int width,int height,int depth,Map<ResourceType,Integer> requiredResources,Map<String,String> blocks,int estimatedTicks){
 public Blueprint{if(id==null||id.isBlank()||name==null||name.isBlank()||description==null||description.isBlank())throw new IllegalArgumentException("text required");Objects.requireNonNull(requiredResources);Objects.requireNonNull(blocks);if(width<=0||height<=0||depth<=0||estimatedTicks<0)throw new IllegalArgumentException("invalid blueprint dimensions");if(requiredResources.values().stream().anyMatch(v->v==null||v<0)||blocks.values().stream().anyMatch(v->v==null||v.isBlank()))throw new IllegalArgumentException("invalid blueprint data");requiredResources=Map.copyOf(requiredResources);blocks=Map.copyOf(blocks);}
 public Blueprint(String id,String name,String description,int width,int height,int depth,Map<ResourceType,Integer> costs,int estimatedTicks){this(id,name,description,width,height,depth,costs,Map.of(),estimatedTicks);}
 public void validate(){if(requiredResources.values().stream().anyMatch(v->v==null||v<0)||blocks.values().stream().anyMatch(v->v==null||v.isBlank()))throw new IllegalArgumentException("invalid blueprint data");}
}
