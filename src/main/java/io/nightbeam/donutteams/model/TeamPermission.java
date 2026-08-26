package io.nightbeam.donutteams.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

public enum TeamPermission {
    INVITE,
    KICK,
    HOME,
    SETHOME,
    SPEAK,
    PVP;

    public static EnumSet<TeamPermission> defaultMember() {
        return EnumSet.of(SPEAK);
    }

    public static EnumSet<TeamPermission> all() {
        return EnumSet.allOf(TeamPermission.class);
    }

    public static String serialize(Set<TeamPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(",");
        for (TeamPermission permission : TeamPermission.values()) {
            if (permissions.contains(permission)) {
                joiner.add(permission.name());
            }
        }
        return joiner.toString();
    }

    public static EnumSet<TeamPermission> deserialize(String raw) {
        EnumSet<TeamPermission> set = EnumSet.noneOf(TeamPermission.class);
        if (raw == null || raw.isBlank()) {
            return set;
        }
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                set.add(TeamPermission.valueOf(trimmed.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Unknown future/pro permission nodes are ignored in lite.
            }
        }
        return set;
    }
}
