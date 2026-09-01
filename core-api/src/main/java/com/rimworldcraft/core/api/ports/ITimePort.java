package com.rimworldcraft.core.api.ports;

/** Provides deterministic game time and scheduling. */
public interface ITimePort {
    /** Returns current game time. */
    long getGameTime();
    /** Returns current game tick. */
    long getTicks();
    /** Schedules an action after a number of ticks. */
    void scheduleAction(Runnable action, long delayTicks);
}
