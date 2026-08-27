package io.nightbeam.donutteams.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Schema {

    private Schema() {
    }

    public static void apply(Connection connection, DatabaseType type) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS teams (
                      id TEXT PRIMARY KEY,
                      name TEXT NOT NULL UNIQUE,
                      tag TEXT NOT NULL,
                      owner_uuid TEXT NOT NULL,
                      created_at BIGINT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS members (
                      team_id TEXT NOT NULL,
                      player_uuid TEXT NOT NULL,
                      player_name TEXT NOT NULL,
                      role TEXT NOT NULL,
                      permissions TEXT NOT NULL,
                      joined_at BIGINT NOT NULL,
                      PRIMARY KEY (player_uuid)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS invites (
                      team_id TEXT NOT NULL,
                      player_uuid TEXT NOT NULL,
                      invited_by TEXT NOT NULL,
                      expires_at BIGINT NOT NULL,
                      PRIMARY KEY (team_id, player_uuid)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS homes (
                      team_id TEXT PRIMARY KEY,
                      world TEXT NOT NULL,
                      x DOUBLE NOT NULL,
                      y DOUBLE NOT NULL,
                      z DOUBLE NOT NULL,
                      yaw FLOAT NOT NULL,
                      pitch FLOAT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS settings (
                      team_id TEXT PRIMARY KEY,
                      pvp_enabled INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_members_team ON members (team_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_invites_player ON invites (player_uuid)");
        }
    }
}
