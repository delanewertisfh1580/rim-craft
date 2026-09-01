package com.rimworldcraft.infrastructure.forge.adapter;

import com.rimworldcraft.core.api.ports.ITimePort;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe clock adapter seam for a Forge server level. */
public final class ForgeTimeAdapter implements ITimePort, AutoCloseable {
    private final Object level;
    private final AtomicLong ticks = new AtomicLong();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread thread = new Thread(r, "rwc-forge-time"); thread.setDaemon(true); return thread; });
    /** Creates a time adapter. */ public ForgeTimeAdapter(Object level) { this.level = Objects.requireNonNull(level); }
    /** Returns the tracked game time. */ public long getGameTime() { return ticks.get(); }
    /** Returns the tracked tick count. */ public long getTicks() { return ticks.get(); }
    /** Schedules an action after the requested tick delay. */ public void scheduleAction(Runnable action, long delayTicks) { if (delayTicks < 0) throw new IllegalArgumentException("delayTicks"); scheduler.schedule(Objects.requireNonNull(action), delayTicks * 50L, TimeUnit.MILLISECONDS); }
    /** Advances the fallback clock by one tick. */ public void advanceTick() { ticks.incrementAndGet(); }
    /** Stops the scheduler. */ public void close() { scheduler.shutdownNow(); }
}
