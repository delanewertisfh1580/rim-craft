package com.rimworldcraft.core.goal;
import java.util.*;
public record Plan(UUID id,Goal goal,List<ActionDefinition> actions,int totalCost,long createdTick){public Plan{Objects.requireNonNull(id);Objects.requireNonNull(goal);actions=List.copyOf(actions);if(actions.size()>32||totalCost<0||createdTick<0)throw new IllegalArgumentException("invalid plan");if(actions.isEmpty()&&goal.type()!=GoalType.IDLE)throw new IllegalArgumentException("empty plan");}}
