package io.nightbeam.donutteams.model;

import java.util.UUID;

public record TeamSettings(UUID teamId, boolean friendlyFire) {

    public static TeamSettings defaults(UUID teamId, boolean friendlyFire) {
        return new TeamSettings(teamId, friendlyFire);
    }
}
