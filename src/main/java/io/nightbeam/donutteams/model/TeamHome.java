package io.nightbeam.donutteams.model;

import java.util.UUID;

public record TeamHome(UUID teamId, String name, String world, double x, double y, double z, float yaw, float pitch,
                       UUID setBy, long createdAtMillis) {

    public static final String DEFAULT = "home";

    public String coordinates() {
        return (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z);
    }
}
