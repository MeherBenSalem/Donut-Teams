package io.nightbeam.donutteams.model;

import java.util.Locale;

/** How one team sees another. TEAMMATE is a player's own team. */
public enum Relation {
    TEAMMATE,
    ALLY,
    NEUTRAL,
    ENEMY;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
