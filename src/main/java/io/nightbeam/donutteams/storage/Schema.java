package io.nightbeam.donutteams.storage;

import io.nightbeam.donutteams.config.PluginSettings;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Schema {

    private Schema() {
    }

    public static void apply(Connection connection, DatabaseType type) throws SQLException {
        boolean mysql = type == DatabaseType.MYSQL;
        String id = mysql ? "CHAR(36)" : "TEXT";
        String name = mysql ? "VARCHAR(32)" : "TEXT";
        String tag = mysql ? "VARCHAR(16)" : "TEXT";
        String world = mysql ? "VARCHAR(64)" : "TEXT";
        String perms = mysql ? "VARCHAR(128)" : "TEXT";
        String engine = mysql ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" : "";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS teams (
                      id %s PRIMARY KEY,
                      name %s NOT NULL UNIQUE,
                      tag %s NOT NULL,
                      owner_uuid %s NOT NULL,
                      created_at BIGINT NOT NULL
                    )%s
                    """.formatted(id, name, tag, id, engine));
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS members (
                      team_id %s NOT NULL,
                      player_uuid %s NOT NULL,
                      player_name %s NOT NULL,
                      role %s NOT NULL,
                      permissions %s NOT NULL,
                      joined_at BIGINT NOT NULL,
                      PRIMARY KEY (player_uuid)
                    )%s
                    """.formatted(id, id, name, name, perms, engine));
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS invites (
                      team_id %s NOT NULL,
                      player_uuid %s NOT NULL,
                      invited_by %s NOT NULL,
                      expires_at BIGINT NOT NULL,
                      PRIMARY KEY (team_id, player_uuid)
                    )%s
                    """.formatted(id, id, id, engine));
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS homes (
                      team_id %s PRIMARY KEY,
                      world %s NOT NULL,
                      x DOUBLE NOT NULL,
                      y DOUBLE NOT NULL,
                      z DOUBLE NOT NULL,
                      yaw FLOAT NOT NULL,
                      pitch FLOAT NOT NULL
                    )%s
                    """.formatted(id, world, engine));
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS settings (
                      team_id %s PRIMARY KEY,
                      pvp_enabled %s NOT NULL DEFAULT 0
                    )%s
                    """.formatted(id, mysql ? "TINYINT" : "INTEGER", engine));
            createIndex(statement, mysql, "idx_members_team", "members", "team_id");
            createIndex(statement, mysql, "idx_invites_player", "invites", "player_uuid");
        }
    }

    private static void createIndex(Statement statement, boolean mysql, String name, String table, String column)
            throws SQLException {
        try {
            if (mysql) {
                statement.executeUpdate("CREATE INDEX " + name + " ON " + table + " (" + column + ")");
            } else {
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS " + name + " ON " + table + " (" + column + ")");
            }
        } catch (SQLException exception) {
            String message = exception.getMessage();
            if (message != null && (message.contains("Duplicate") || message.contains("already exists"))) {
                return;
            }
            throw exception;
        }
    }

    public static DatabaseType fromSettings(PluginSettings settings) {
        try {
            return DatabaseType.valueOf(settings.storageType().trim().toUpperCase());
        } catch (Exception ignored) {
            return DatabaseType.SQLITE;
        }
    }
}
