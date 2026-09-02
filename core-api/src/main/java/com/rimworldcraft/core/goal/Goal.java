package com.rimworldcraft.core.goal;
import java.util.Objects;
public record Goal(GoalType type,int priority,String targetKey){public Goal{Objects.requireNonNull(type);if(priority<1||priority>100)throw new IllegalArgumentException("priority 1..100");targetKey=targetKey==null?"":targetKey;}}
