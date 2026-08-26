package io.nightbeam.donutteams.api;

import java.util.UUID;

public record TeamSnapshot(
        UUID id,
        String name,
        String tag,
        UUID ownerId,
        int memberCount,
        boolean friendlyFire,
        String homeWorld
) {
}
