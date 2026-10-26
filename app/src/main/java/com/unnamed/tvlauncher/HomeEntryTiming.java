package com.unnamed.tvlauncher;

/** Small pure-Java timing helper so the one-second Home intro duration can be unit-tested. */
final class HomeEntryTiming {
    private HomeEntryTiming() { }

    /** Returns the remaining delay needed to make the intro last at least durationMs. */
    static long remainingMillis(long startedAtElapsedRealtime, long nowElapsedRealtime, long durationMs) {
        long elapsed = Math.max(0L, nowElapsedRealtime - startedAtElapsedRealtime);
        return Math.max(0L, durationMs - elapsed);
    }
}
