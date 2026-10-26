package io.nightbeam.donutteams.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Creates the tables and upgrades older ones. Version 1 is the 1.0.x layout (SQLite only): one home per team and a
 * separate settings table. Version 2 is 2.0. The version is kept in the {@code meta} table.
 */
public final class Schema {

    public static final int VERSION = 2;

    private Schema() {
    }

    /** Brings the database to {@link #VERSION}. Returns the version it found (0 = new database). */
    public static int apply(Connection connection, DatabaseType type, String prefix, Consumer<String> log) throws SQLException {
        Tables t = new Tables(prefix);
        int found = detect(connection, t);
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if (found == 1) {
                log.accept("Upgrading the 1.x database to 2.0 (teams, members, invites and homes are kept)");
                upgradeFromV1(connection, type, t);
            }
            create(connection, type, t);
            setVersion(connection, t, VERSION);
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
        return found;
    }

    /** 0 = empty, 1 = the 1.x layout, otherwise the stored version. */
    static int detect(Connection connection, Tables t) throws SQLException {
        if (tableExists(connection, t.meta)) {
            try (Statement statement = connection.createStatement();
                    ResultSet results = statement.executeQuery("SELECT v FROM " + t.meta + " WHERE k = 'schema_version'")) {
                if (results.next()) {
                    try {
                        return Integer.parseInt(results.getString(1).trim());
                    } catch (NumberFormatException ignored) {
                        return VERSION;
                    }
                }
            }
        }
        return tableExists(connection, t.teams) ? 1 : 0;
    }

    private static void create(Connection connection, DatabaseType type, Tables t) throws SQLException {
        boolean maria = type == DatabaseType.MARIADB;
        String engine = maria ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" : "";
        List<String> ddl = new ArrayList<>();
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.meta + " (k VARCHAR(64) NOT NULL PRIMARY KEY, v VARCHAR(255) NOT NULL)" + engine);
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.teams + " ("
                + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "name VARCHAR(32) NOT NULL UNIQUE, "
                + "tag VARCHAR(16) NOT NULL, "
                + "owner_uuid VARCHAR(36) NOT NULL, "
                + "created_at BIGINT NOT NULL, "
                + "description VARCHAR(255) NOT NULL DEFAULT '', "
                + "motd VARCHAR(255) NOT NULL DEFAULT '', "
                + "color VARCHAR(16) NOT NULL DEFAULT 'GOLD', "
                + "icon TEXT NULL, "
                + "open_join INT NOT NULL DEFAULT 0, "
                + "friendly_fire INT NOT NULL DEFAULT 0, "
                + "ally_friendly_fire INT NOT NULL DEFAULT 0, "
                + "team_level INT NOT NULL DEFAULT 1, "
                + "balance DOUBLE NOT NULL DEFAULT 0, "
                + "member_cap INT NOT NULL DEFAULT 0)" + engine);
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.members + " ("
                + "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "team_id VARCHAR(36) NOT NULL, "
                + "player_name VARCHAR(64) NOT NULL, "
                + "role VARCHAR(16) NOT NULL, "
                + "permissions VARCHAR(255) NOT NULL, "
                + "joined_at BIGINT NOT NULL, "
                + "last_seen BIGINT NOT NULL DEFAULT 0, "
                + "kills INT NOT NULL DEFAULT 0, "
                + "deaths INT NOT NULL DEFAULT 0"
                + (maria ? ", INDEX " + t.members + "_team (team_id)" : "") + ")" + engine);
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.invites + " ("
                + "team_id VARCHAR(36) NOT NULL, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "invited_by VARCHAR(36) NOT NULL, "
                + "expires_at BIGINT NOT NULL, "
                + "PRIMARY KEY (team_id, player_uuid)"
                + (maria ? ", INDEX " + t.invites + "_player (player_uuid)" : "") + ")" + engine);
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.homes + " ("
                + "team_id VARCHAR(36) NOT NULL, "
                + "name VARCHAR(32) NOT NULL, "
                + "world VARCHAR(64) NOT NULL, "
                + "x DOUBLE NOT NULL, "
                + "y DOUBLE NOT NULL, "
                + "z DOUBLE NOT NULL, "
                + "yaw FLOAT NOT NULL, "
                + "pitch FLOAT NOT NULL, "
                + "set_by VARCHAR(36) NULL, "
                + "created_at BIGINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (team_id, name))" + engine);
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.relations + " ("
                + "team_id VARCHAR(36) NOT NULL, "
                + "other_id VARCHAR(36) NOT NULL, "
                + "kind VARCHAR(8) NOT NULL, "
                + "created_at BIGINT NOT NULL, "
                + "PRIMARY KEY (team_id, other_id))" + engine);
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.chests + " ("
                + "team_id VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "contents " + (maria ? "MEDIUMTEXT" : "TEXT") + " NOT NULL, "
                + "updated_at BIGINT NOT NULL)" + engine);
        ddl.add("CREATE TABLE IF NOT EXISTS " + t.bankLog + " ("
                + (maria ? "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " : "id INTEGER PRIMARY KEY AUTOINCREMENT, ")
                + "team_id VARCHAR(36) NOT NULL, "
                + "player_uuid VARCHAR(36) NULL, "
                + "player_name VARCHAR(64) NOT NULL DEFAULT '', "
                + "kind VARCHAR(16) NOT NULL, "
                + "amount DOUBLE NOT NULL, "
                + "balance_after DOUBLE NOT NULL, "
                + "at BIGINT NOT NULL"
                + (maria ? ", INDEX " + t.bankLog + "_team (team_id, id)" : "") + ")" + engine);
        if (!maria) {
            ddl.add("CREATE INDEX IF NOT EXISTS " + t.members + "_team ON " + t.members + " (team_id)");
            ddl.add("CREATE INDEX IF NOT EXISTS " + t.invites + "_player ON " + t.invites + " (player_uuid)");
            ddl.add("CREATE INDEX IF NOT EXISTS " + t.bankLog + "_team ON " + t.bankLog + " (team_id, id)");
        }
        try (Statement statement = connection.createStatement()) {
            for (String sql : ddl) {
                statement.executeUpdate(sql);
            }
        }
    }

    /**
     * 1.x (SQLite): adds the 2.0 columns to teams and members, moves friendly fire from the settings table into
     * teams, and turns the one-home table into named homes (the 1.x home is called "home").
     */
    private static void upgradeFromV1(Connection connection, DatabaseType type, Tables t) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            addColumn(connection, statement, t.teams, "description", "VARCHAR(255) NOT NULL DEFAULT ''");
            addColumn(connection, statement, t.teams, "motd", "VARCHAR(255) NOT NULL DEFAULT ''");
            addColumn(connection, statement, t.teams, "color", "VARCHAR(16) NOT NULL DEFAULT 'GOLD'");
            addColumn(connection, statement, t.teams, "icon", "TEXT NULL");
            addColumn(connection, statement, t.teams, "open_join", "INT NOT NULL DEFAULT 0");
            addColumn(connection, statement, t.teams, "friendly_fire", "INT NOT NULL DEFAULT 0");
            addColumn(connection, statement, t.teams, "ally_friendly_fire", "INT NOT NULL DEFAULT 0");
            addColumn(connection, statement, t.teams, "team_level", "INT NOT NULL DEFAULT 1");
            addColumn(connection, statement, t.teams, "balance", "DOUBLE NOT NULL DEFAULT 0");
            addColumn(connection, statement, t.teams, "member_cap", "INT NOT NULL DEFAULT 0");
            addColumn(connection, statement, t.members, "last_seen", "BIGINT NOT NULL DEFAULT 0");
            addColumn(connection, statement, t.members, "kills", "INT NOT NULL DEFAULT 0");
            addColumn(connection, statement, t.members, "deaths", "INT NOT NULL DEFAULT 0");

            String settings = t.prefix + "settings";
            if (tableExists(connection, settings)) {
                statement.executeUpdate("UPDATE " + t.teams + " SET friendly_fire = COALESCE((SELECT s.pvp_enabled FROM "
                        + settings + " s WHERE s.team_id = " + t.teams + ".id), 0)");
                statement.executeUpdate("DROP TABLE " + settings);
            }

            if (tableExists(connection, t.homes) && !columnExists(connection, t.homes, "name")) {
                String old = t.homes + "_v1";
                statement.executeUpdate("ALTER TABLE " + t.homes + " RENAME TO " + old);
                create(connection, type, t);
                statement.executeUpdate("INSERT INTO " + t.homes + " (team_id, name, world, x, y, z, yaw, pitch, set_by, created_at) "
                        + "SELECT team_id, 'home', world, x, y, z, yaw, pitch, NULL, 0 FROM " + old);
                statement.executeUpdate("DROP TABLE " + old);
            }
        }
        // Members upgraded from 1.x get the permissions their rank grants for features 1.x did not have. They are
        // added by the plugin at load time (see TeamRepository#loadAll), because the defaults live in config.yml.
        setMeta(connection, t, "upgraded_from", "1");
    }

    private static void addColumn(Connection connection, Statement statement, String table, String column, String definition)
            throws SQLException {
        if (!columnExists(connection, table, column)) {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    static boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        for (String candidate : new String[] {table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)}) {
            try (ResultSet results = meta.getTables(connection.getCatalog(), null, candidate, new String[] {"TABLE"})) {
                if (results.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery("SELECT * FROM " + table + " WHERE 1 = 0")) {
            var meta = results.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if (meta.getColumnName(i).equalsIgnoreCase(column)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void setVersion(Connection connection, Tables t, int version) throws SQLException {
        setMeta(connection, t, "schema_version", String.valueOf(version));
    }

    static void setMeta(Connection connection, Tables t, String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + t.meta + " (k, v) VALUES (?, ?)")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    static String meta(Connection connection, Tables t, String key) throws SQLException {
        if (!tableExists(connection, t.meta)) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT v FROM " + t.meta + " WHERE k = ?")) {
            statement.setString(1, key);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getString(1) : null;
            }
        }
    }
}
