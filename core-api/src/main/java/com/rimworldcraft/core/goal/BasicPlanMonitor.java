package com.rimworldcraft.core.goal;

/** Default bounded monitor for immutable plans and observations. */
public final class BasicPlanMonitor implements PlanMonitor {
    @Override
    public boolean valid(Plan plan, WorldState state) {
        return plan != null && state != null && plan.goal().priority() >= 1 && plan.actions().size() <= 32;
    }

    @Override
    public boolean timedOut(long startedTick, long nowTick, long timeoutTicks) {
        if (startedTick < 0 || nowTick < startedTick || timeoutTicks < 0) return true;
        return nowTick - startedTick >= timeoutTicks;
    }
}
