package com.rimworldcraft.core.goal;
public interface PlanMonitor { boolean valid(Plan plan,WorldState state); boolean timedOut(long startedTick,long nowTick,long timeoutTicks); }
