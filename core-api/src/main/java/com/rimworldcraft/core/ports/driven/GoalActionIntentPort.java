package com.rimworldcraft.core.ports.driven;
import com.rimworldcraft.core.goal.ActionDefinition;
import com.rimworldcraft.core.goal.WorldState;
public interface GoalActionIntentPort { void submit(ActionDefinition action,WorldState state); void cancel(String executionId); }
