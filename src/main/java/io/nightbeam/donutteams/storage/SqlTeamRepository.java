package io.nightbeam.donutteams.storage;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.model.TeamSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

public final class SqlTeamRepository {

    private final DataSource dataSource;
    private final DatabaseType type;

    public SqlTeamRepository(DataSource dataSource, DatabaseType type) {
        this.dataSource = dataSource;
        this.type = type;
    }

    public List<Team> loadAll() throws SQLException {
        List<Team> teams = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, name, tag, owner_uuid, created_at FROM teams");
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                teams.add(readTeam(results));
            }
        }
        for (Team team : teams) {
            for (TeamMember member : loadMembers(team.id())) {
                team.putMember(member);
            }
            team.home(loadHome(team.id()));
            TeamSettings settings = loadSettings(team.id());
            if (settings != null) {
                team.settings(settings);
            }
        }
        return teams;
    }

    public List<TeamInvite> loadInvites() throws SQLException {
        List<TeamInvite> invites = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT team_id, player_uuid, invited_by, expires_at FROM invites");
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                invites.add(new TeamInvite(
                        uuid(results.getString("team_id")),
                        uuid(results.getString("player_uuid")),
                        uuid(results.getString("invited_by")),
                        results.getLong("expires_at")));
            }
        }
        return invites;
    }

    public void insertTeam(Team team, TeamMember owner, TeamSettings settings) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO teams (id, name, tag, owner_uuid, created_at) VALUES (?, ?, ?, ?, ?)")) {
                    statement.setString(1, team.id().toString());
                    statement.setString(2, team.name());
                    statement.setString(3, team.tag());
                    statement.setString(4, team.ownerId().toString());
                    statement.setLong(5, team.createdAtMillis());
                    statement.executeUpdate();
                }
                insertMember(connection, team.id(), owner);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO settings (team_id, pvp_enabled) VALUES (?, ?)")) {
                    statement.setString(1, team.id().toString());
                    statement.setInt(2, settings.friendlyFire() ? 1 : 0);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void deleteTeam(UUID teamId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM invites WHERE team_id = ?")) {
                    statement.setString(1, teamId.toString());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM homes WHERE team_id = ?")) {
                    statement.setString(1, teamId.toString());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM settings WHERE team_id = ?")) {
                    statement.setString(1, teamId.toString());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM members WHERE team_id = ?")) {
                    statement.setString(1, teamId.toString());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM teams WHERE id = ?")) {
                    statement.setString(1, teamId.toString());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void upsertMember(UUID teamId, TeamMember member) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            insertMember(connection, teamId, member);
        }
    }

    public void deleteMember(UUID playerId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM members WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        }
    }

    public void updateOwner(UUID teamId, UUID ownerId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("UPDATE teams SET owner_uuid = ? WHERE id = ?")) {
            statement.setString(1, ownerId.toString());
            statement.setString(2, teamId.toString());
            statement.executeUpdate();
        }
    }

    public void updateMemberName(UUID playerId, String name) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE members SET player_name = ? WHERE player_uuid = ?")) {
            statement.setString(1, name);
            statement.setString(2, playerId.toString());
            statement.executeUpdate();
        }
    }

    public void insertInvite(TeamInvite invite) throws SQLException {
        String sql = """
                INSERT INTO invites (team_id, player_uuid, invited_by, expires_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(team_id, player_uuid) DO UPDATE SET invited_by = excluded.invited_by, expires_at = excluded.expires_at
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invite.teamId().toString());
            statement.setString(2, invite.playerId().toString());
            statement.setString(3, invite.invitedBy().toString());
            statement.setLong(4, invite.expiresAtMillis());
            statement.executeUpdate();
        }
    }

    public void deleteInvite(UUID teamId, UUID playerId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM invites WHERE team_id = ? AND player_uuid = ?")) {
            statement.setString(1, teamId.toString());
            statement.setString(2, playerId.toString());
            statement.executeUpdate();
        }
    }

    public void deleteExpiredInvites(long now) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM invites WHERE expires_at < ?")) {
            statement.setLong(1, now);
            statement.executeUpdate();
        }
    }

    public void upsertHome(TeamHome home) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement delete = connection.prepareStatement("DELETE FROM homes WHERE team_id = ?");
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO homes (team_id, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            delete.setString(1, home.teamId().toString());
            delete.executeUpdate();
            insert.setString(1, home.teamId().toString());
            insert.setString(2, home.world());
            insert.setDouble(3, home.x());
            insert.setDouble(4, home.y());
            insert.setDouble(5, home.z());
            insert.setFloat(6, home.yaw());
            insert.setFloat(7, home.pitch());
            insert.executeUpdate();
        }
    }

    public void deleteHome(UUID teamId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM homes WHERE team_id = ?")) {
            statement.setString(1, teamId.toString());
            statement.executeUpdate();
        }
    }

    public void upsertSettings(TeamSettings settings) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement delete = connection.prepareStatement("DELETE FROM settings WHERE team_id = ?");
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO settings (team_id, pvp_enabled) VALUES (?, ?)")) {
            delete.setString(1, settings.teamId().toString());
            delete.executeUpdate();
            insert.setString(1, settings.teamId().toString());
            insert.setInt(2, settings.friendlyFire() ? 1 : 0);
            insert.executeUpdate();
        }
    }

    private List<TeamMember> loadMembers(UUID teamId) throws SQLException {
        List<TeamMember> members = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT player_uuid, player_name, role, permissions, joined_at FROM members WHERE team_id = ?")) {
            statement.setString(1, teamId.toString());
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    members.add(new TeamMember(
                            uuid(results.getString("player_uuid")),
                            results.getString("player_name"),
                            TeamRole.valueOf(results.getString("role")),
                            TeamPermission.deserialize(results.getString("permissions")),
                            results.getLong("joined_at")));
                }
            }
        }
        return members;
    }

    private TeamHome loadHome(UUID teamId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT world, x, y, z, yaw, pitch FROM homes WHERE team_id = ?")) {
            statement.setString(1, teamId.toString());
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return new TeamHome(
                        teamId,
                        results.getString("world"),
                        results.getDouble("x"),
                        results.getDouble("y"),
                        results.getDouble("z"),
                        results.getFloat("yaw"),
                        results.getFloat("pitch"));
            }
        }
    }

    private TeamSettings loadSettings(UUID teamId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT pvp_enabled FROM settings WHERE team_id = ?")) {
            statement.setString(1, teamId.toString());
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return TeamSettings.defaults(teamId, false);
                }
                return new TeamSettings(teamId, results.getInt("pvp_enabled") != 0);
            }
        }
    }

    private void insertMember(Connection connection, UUID teamId, TeamMember member) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM members WHERE player_uuid = ?");
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO members (team_id, player_uuid, player_name, role, permissions, joined_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            delete.setString(1, member.playerId().toString());
            delete.executeUpdate();
            insert.setString(1, teamId.toString());
            insert.setString(2, member.playerId().toString());
            insert.setString(3, member.name());
            insert.setString(4, member.role().name());
            insert.setString(5, TeamPermission.serialize(member.permissions()));
            insert.setLong(6, member.joinedAtMillis());
            insert.executeUpdate();
        }
    }

    private static Team readTeam(ResultSet results) throws SQLException {
        return new Team(
                uuid(results.getString("id")),
                results.getString("name"),
                results.getString("tag"),
                uuid(results.getString("owner_uuid")),
                results.getLong("created_at"));
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value);
    }
}
