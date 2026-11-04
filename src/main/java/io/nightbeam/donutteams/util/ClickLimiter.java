package io.nightbeam.donutteams.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drops menu clicks that come faster than a cooldown, per player, so an autoclicker can't make the server rebuild
 * menus and save data hundreds of times a second. Thread-safe: on Folia each player's clicks run on their own region.
 */
public final class ClickLimiter {

    private final Map<UUID, Long> lastAccepted = new ConcurrentHashMap<>();

    /** True when this click is at least {@code cooldownMillis} after the player's last accepted one. */
    public boolean accept(UUID player, long now, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return true;
        }
        Long last = lastAccepted.get(player);
        if (last != null && now >= last && now - last < cooldownMillis) {
            return false;
        }
        lastAccepted.put(player, now);
        return true;
    }

    public void forget(UUID player) {
        lastAccepted.remove(player);
    }
}
