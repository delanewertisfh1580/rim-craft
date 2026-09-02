package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.goal.GoalAiConfig;

/** Supplies an immutable configuration snapshot to Goal AI application services. */
@FunctionalInterface
public interface GoalConfigPort {
    GoalAiConfig snapshot();
}
