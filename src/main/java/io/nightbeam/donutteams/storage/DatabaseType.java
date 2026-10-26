package io.nightbeam.donutteams.storage;

import java.util.Locale;

public enum DatabaseType {
    SQLITE,
    MARIADB;

    /** MYSQL is accepted as MARIADB (the MariaDB driver talks to both). */
    public static DatabaseType parse(String raw) {
        if (raw == null) {
            return SQLITE;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("MYSQL") || value.equals("MARIADB")) {
            return MARIADB;
        }
        return SQLITE;
    }
}
