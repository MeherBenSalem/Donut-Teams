package io.nightbeam.donutteams.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

/** What a member may do in the team. Owners have every permission. */
public enum TeamPermission {
    INVITE,
    KICK,
    HOME,
    SETHOME,
    SPEAK,
    PVP,
    BANK_DEPOSIT,
    BANK_WITHDRAW,
    CHEST,
    RELATIONS,
    SETTINGS,
    LEVEL_UP;

    /** Permissions that did not exist in 1.x. Members upgraded from 1.x get the ones their role grants by default. */
    public static final Set<TeamPermission> ADDED_IN_2_0 = EnumSet.of(BANK_DEPOSIT, BANK_WITHDRAW, CHEST, RELATIONS, SETTINGS, LEVEL_UP);

    /** The 1.x default for a member (team chat only). */
    public static EnumSet<TeamPermission> defaultMember() {
        return EnumSet.of(SPEAK);
    }

    public static EnumSet<TeamPermission> all() {
        return EnumSet.allOf(TeamPermission.class);
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static TeamPermission parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TeamPermission.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static EnumSet<TeamPermission> parseAll(Collection<String> raw) {
        EnumSet<TeamPermission> set = EnumSet.noneOf(TeamPermission.class);
        if (raw == null) {
            return set;
        }
        for (String value : raw) {
            if ("*".equals(value == null ? null : value.trim())) {
                return all();
            }
            TeamPermission permission = parse(value);
            if (permission != null) {
                set.add(permission);
            }
        }
        return set;
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
            TeamPermission permission = parse(token);
            if (permission != null) {
                set.add(permission);
            }
        }
        return set;
    }
}
