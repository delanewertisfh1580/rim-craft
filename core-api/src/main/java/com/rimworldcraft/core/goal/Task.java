package com.rimworldcraft.core.goal;
import java.util.*;
public record Task(UUID id,String type,int priority,long timeoutTick){public Task{Objects.requireNonNull(id);if(type==null||type.isBlank()||priority<1||priority>100||timeoutTick<0)throw new IllegalArgumentException("invalid task");}}
