package io.nightbeam.donutteams.api;

import java.util.Set;
import java.util.UUID;

/**
 * A read-only copy of a team at the moment it was taken.
 *
 * @param color the team colour as a MiniMessage colour name (e.g. {@code gold})
 */
public record TeamSnapshot(
        UUID id,
        String name,
        String tag,
        UUID ownerId,
        int memberCount,
        boolean friendlyFire,
        String homeWorld,
        String color,
        int level,
        double balance,
        int kills,
        int deaths,
        boolean open,
        Set<UUID> allies,
        Set<UUID> enemies,
        Set<UUID> members
) {
}
