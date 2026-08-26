package io.nightbeam.donutteams.model;

import java.util.UUID;

public record TeamHome(UUID teamId, String world, double x, double y, double z, float yaw, float pitch) {
}
