package io.nightbeam.donutteams.model;

import java.util.Locale;

/** Team ranks, highest first. A member can only manage members of a lower rank. */
public enum TeamRole {
    OWNER(3),
    OFFICER(2),
    MEMBER(1);

    private final int weight;

    TeamRole(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean outranks(TeamRole other) {
        return other == null || weight > other.weight;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses stored roles; 1.x only knew OWNER and MEMBER. Unknown values fall back to MEMBER. */
    public static TeamRole parse(String raw) {
        if (raw == null) {
            return MEMBER;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("ADMIN") || value.equals("MOD") || value.equals("MODERATOR")) {
            return OFFICER;
        }
        try {
            return TeamRole.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return MEMBER;
        }
    }
}
