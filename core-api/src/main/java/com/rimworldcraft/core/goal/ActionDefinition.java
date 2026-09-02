package com.rimworldcraft.core.goal;
import java.util.*;
public record ActionDefinition(String id,ActionType type,int cost,int durationTicks,Set<StateFact> preconditions,Set<StateFact> effects){public ActionDefinition{if(id==null||id.isBlank()||cost<0||durationTicks<0)throw new IllegalArgumentException("invalid action");preconditions=Set.copyOf(preconditions);effects=Set.copyOf(effects);}}
