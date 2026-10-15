package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** Bridges Better Days' morning notification to vanilla's subsequent clock adjustment. */
public final class BetterDaysWakeTime {
    // Both hooks run on the server thread. A pending wake belongs to one level only.
    private static final Map<ServerLevel, Long> PENDING = new WeakHashMap<>();

    private BetterDaysWakeTime() {
    }

    public static void schedule(ServerLevel level) {
        if (!SereneExtendedConfig.ENABLE_BETTER_DAYS_SLEEP_WAKE_TIME_FIX.get()
                || level.dimensionType().defaultClock().isEmpty()) {
            PENDING.remove(level);
            return;
        }
        long currentTime = level.getDefaultClockTime();
        long wakeTime = Math.floorDiv(currentTime, 24000L) * 24000L
                + SereneExtendedConfig.BETTER_DAYS_SLEEP_WAKE_TIME.get();
        if (wakeTime <= currentTime) wakeTime += 24000L;
        PENDING.put(level, wakeTime);
    }

    public static void apply(SleepFinishedTimeEvent event) {
        Long wakeTime = PENDING.remove(event.getLevel());
        if (wakeTime != null && !event.isCanceled()
                && SereneExtendedConfig.ENABLE_BETTER_DAYS_SLEEP_WAKE_TIME_FIX.get()) {
            event.setAdjustment(new ClockAdjustment.Absolute(wakeTime));
        }
    }

    public static void clear() {
        PENDING.clear();
    }
}
