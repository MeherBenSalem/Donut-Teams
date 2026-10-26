package io.nightbeam.donutteams.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

class SchemaTest {

    @TempDir
    Path folder;

    private SQLiteDataSource sqlite(String name) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + folder.resolve(name).toAbsolutePath());
        return dataSource;
    }

    @Test
    void newDatabaseGetsEveryTableAtTheCurrentVersion() throws Exception {
        SQLiteDataSource dataSource = sqlite("new.db");
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0, Schema.apply(connection, DatabaseType.SQLITE, "", message -> { }));
            for (String table : List.of("meta", "teams", "members", "invites", "homes", "relations", "chests", "bank_log")) {
                assertTrue(Schema.tableExists(connection, table), table);
            }
            assertEquals(Schema.VERSION, Schema.detect(connection, new Tables("")));
            // Applying again changes nothing.
            assertEquals(Schema.VERSION, Schema.apply(connection, DatabaseType.SQLITE, "", message -> { }));
        }
    }

    @Test
    void oneDotXDatabaseIsUpgradedInPlace() throws Exception {
        SQLiteDataSource dataSource = sqlite("v1.db");
        UUID teamId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            // The exact 1.0.1 layout.
            statement.executeUpdate("CREATE TABLE teams (id TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE, tag TEXT NOT NULL, owner_uuid TEXT NOT NULL, created_at BIGINT NOT NULL)");
            statement.executeUpdate("CREATE TABLE members (team_id TEXT NOT NULL, player_uuid TEXT NOT NULL, player_name TEXT NOT NULL, role TEXT NOT NULL, permissions TEXT NOT NULL, joined_at BIGINT NOT NULL, PRIMARY KEY (player_uuid))");
            statement.executeUpdate("CREATE TABLE invites (team_id TEXT NOT NULL, player_uuid TEXT NOT NULL, invited_by TEXT NOT NULL, expires_at BIGINT NOT NULL, PRIMARY KEY (team_id, player_uuid))");
            statement.executeUpdate("CREATE TABLE homes (team_id TEXT PRIMARY KEY, world TEXT NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL)");
            statement.executeUpdate("CREATE TABLE settings (team_id TEXT PRIMARY KEY, pvp_enabled INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("INSERT INTO teams VALUES ('" + teamId + "', 'Nightbeam', 'NB', '" + owner + "', 1000)");
            statement.executeUpdate("INSERT INTO members VALUES ('" + teamId + "', '" + owner + "', 'Moonbeam', 'OWNER', 'INVITE,KICK,HOME,SETHOME,SPEAK,PVP', 1000)");
            statement.executeUpdate("INSERT INTO members VALUES ('" + teamId + "', '" + member + "', 'Kiri', 'MEMBER', 'SPEAK,HOME', 2000)");
            statement.executeUpdate("INSERT INTO homes VALUES ('" + teamId + "', 'world', 10.5, 64, -20.5, 90, 0)");
            statement.executeUpdate("INSERT INTO settings VALUES ('" + teamId + "', 1)");
            assertEquals(1, Schema.apply(connection, DatabaseType.SQLITE, "", message -> { }));
            assertFalse(Schema.tableExists(connection, "settings"));
        }
        TeamRepository repository = new TeamRepository(dataSource, DatabaseType.SQLITE, "");
        List<Team> teams = repository.loadAll();
        assertEquals(1, teams.size());
        Team team = teams.get(0);
        assertEquals("Nightbeam", team.name());
        assertTrue(team.friendlyFire(), "friendly fire moves from the settings table");
        assertEquals(1, team.level());
        assertEquals(2, team.size());
        assertEquals(TeamRole.OWNER, team.member(owner).role());
        assertTrue(team.member(member).has(TeamPermission.HOME));
        TeamHome home = team.home("home");
        assertNotNull(home, "the 1.x home is now called home");
        assertEquals(10.5, home.x());
        assertEquals("1", repository.meta("upgraded_from"));
    }

    @Test
    void repositoryRoundTrips() throws Exception {
        SQLiteDataSource dataSource = sqlite("round.db");
        try (Connection connection = dataSource.getConnection()) {
            Schema.apply(connection, DatabaseType.SQLITE, "dt_", message -> { });
        }
        RepositoryContract.run(new TeamRepository(dataSource, DatabaseType.SQLITE, "dt_"));
    }
}
