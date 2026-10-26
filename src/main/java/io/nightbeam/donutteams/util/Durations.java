package io.nightbeam.donutteams.util;

/** Short human durations: {@code 4m 12s}, {@code 3d 2h}. */
public final class Durations {

    private Durations() {
    }

    public static String compact(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        long days = seconds / 86_400;
        long hours = seconds % 86_400 / 3600;
        long minutes = seconds % 3600 / 60;
        long secs = seconds % 60;
        if (days > 0) {
            return days + "d" + (hours > 0 ? " " + hours + "h" : "");
        }
        if (hours > 0) {
            return hours + "h" + (minutes > 0 ? " " + minutes + "m" : "");
        }
        if (minutes > 0) {
            return minutes + "m" + (secs > 0 ? " " + secs + "s" : "");
        }
        return secs + "s";
    }

    /** {@code 5m} ago style, for "last seen". */
    public static String ago(long thenMillis, long nowMillis) {
        long delta = Math.max(0, nowMillis - thenMillis);
        if (delta < 60_000) {
            return "now";
        }
        long seconds = delta / 1000;
        if (seconds < 3600) {
            return (seconds / 60) + "m";
        }
        if (seconds < 86_400) {
            return (seconds / 3600) + "h";
        }
        return (seconds / 86_400) + "d";
    }
}
