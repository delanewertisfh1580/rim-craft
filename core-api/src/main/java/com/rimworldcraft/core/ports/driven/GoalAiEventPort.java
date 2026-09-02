package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.goal.PlanFailureEvent;

/** Publishes immutable Goal AI results; subscribers own their aggregate mutations. */
@FunctionalInterface
public interface GoalAiEventPort {
    void publishPlanFailure(PlanFailureEvent event);
}
