package io.nightbeam.donutteams.storage;

import io.nightbeam.donutteams.model.BankEntry;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamColor;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

/**
 * SQL for teams, members, invites, homes, relations, chests and bank history. Every method blocks; the team service
 * calls them from its database thread (and once at start-up to load everything).
 */
public final class TeamRepository {

    private final DataSource dataSource;
    private final DatabaseType type;
    private final Tables t;

    public TeamRepository(DataSource dataSource, DatabaseType type, String prefix) {
        this.dataSource = dataSource;
        this.type = type;
        this.t = new Tables(prefix);
    }

    public DatabaseType type() {
        return type;
    }

    // ── Loading ─────────────────────────────────────────────────────────────────────────────────────────────

    /** Every team with its members, homes and relations. Four queries, whatever the number of teams. */
    public List<Team> loadAll() throws SQLException {
        Map<UUID, Team> teams = new HashMap<>();
        List<Team> ordered = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement();
                    ResultSet results = statement.executeQuery("SELECT id, name, tag, owner_uuid, created_at, description, motd, color, "
                            + "icon, open_join, friendly_fire, ally_friendly_fire, team_level, balance, member_cap FROM " + t.teams)) {
                while (results.next()) {
                    Team team = new Team(uuid(results.getString("id")), results.getString("name"), results.getString("tag"),
                            uuid(results.getString("owner_uuid")), results.getLong("created_at"));
                    team.description(results.getString("description"));
                    team.motd(results.getString("motd"));
                    team.color(TeamColor.parse(results.getString("color"), TeamColor.GOLD));
                    team.iconData(results.getString("icon"));
                    team.open(results.getInt("open_join") != 0);
                    team.friendlyFire(results.getInt("friendly_fire") != 0);
                    team.allyFriendlyFire(results.getInt("ally_friendly_fire") != 0);
                    team.level(results.getInt("team_level"));
                    team.balance(results.getDouble("balance"));
                    team.memberCap(results.getInt("member_cap"));
                    teams.put(team.id(), team);
                    ordered.add(team);
                }
            }
            try (Statement statement = connection.createStatement();
                    ResultSet results = statement.executeQuery("SELECT team_id, player_uuid, player_name, role, permissions, joined_at, "
                            + "last_seen, kills, deaths FROM " + t.members)) {
                while (results.next()) {
                    Team team = teams.get(uuid(results.getString("team_id")));
                    if (team == null) {
                        continue;
                    }
                    long joined = results.getLong("joined_at");
                    long seen = results.getLong("last_seen");
                    team.putMember(new TeamMember(
                            uuid(results.getString("player_uuid")),
                            results.getString("player_name"),
                            TeamRole.parse(results.getString("role")),
                            TeamPermission.deserialize(results.getString("permissions")),
                            joined,
                            seen <= 0 ? joined : seen,
                            results.getInt("kills"),
                            results.getInt("deaths")));
                }
            }
            try (Statement statement = connection.createStatement();
                    ResultSet results = statement.executeQuery("SELECT team_id, name, world, x, y, z, yaw, pitch, set_by, created_at FROM "
                            + t.homes)) {
                while (results.next()) {
                    Team team = teams.get(uuid(results.getString("team_id")));
                    if (team == null) {
                        continue;
                    }
                    String setBy = results.getString("set_by");
                    team.putHome(new TeamHome(team.id(), results.getString("name"), results.getString("world"),
                            results.getDouble("x"), results.getDouble("y"), results.getDouble("z"),
                            results.getFloat("yaw"), results.getFloat("pitch"),
                            setBy == null || setBy.isBlank() ? null : uuid(setBy), results.getLong("created_at")));
                }
            }
            try (Statement statement = connection.createStatement();
                    ResultSet results = statement.executeQuery("SELECT team_id, other_id, kind FROM " + t.relations)) {
                while (results.next()) {
                    Team team = teams.get(uuid(results.getString("team_id")));
                    UUID other = uuid(results.getString("other_id"));
                    if (team == null || !teams.containsKey(other)) {
                        continue;
                    }
                    if ("ALLY".equalsIgnoreCase(results.getString("kind"))) {
                        team.addAlly(other);
                    } else {
                        team.addEnemy(other);
                    }
                }
            }
        }
        return ordered;
    }

    public List<TeamInvite> loadInvites() throws SQLException {
        List<TeamInvite> invites = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT team_id, player_uuid, invited_by, expires_at FROM " + t.invites + " WHERE expires_at > ?")) {
            statement.setLong(1, System.currentTimeMillis());
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    invites.add(new TeamInvite(uuid(results.getString("team_id")), uuid(results.getString("player_uuid")),
                            uuid(results.getString("invited_by")), results.getLong("expires_at")));
                }
            }
        }
        return invites;
    }

    public String meta(String key) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return Schema.meta(connection, t, key);
        }
    }

    public void meta(String key, String value) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Schema.setMeta(connection, t, key, value);
        }
    }

    // ── Teams ───────────────────────────────────────────────────────────────────────────────────────────────

    /** Inserts or updates the team row (not its members or homes). */
    public void saveTeam(Team team) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            saveTeam(connection, team);
        }
    }

    private void saveTeam(Connection connection, Team team) throws SQLException {
        String sql = "REPLACE INTO " + t.teams + " (id, name, tag, owner_uuid, created_at, description, motd, color, icon, "
                + "open_join, friendly_fire, ally_friendly_fire, team_level, balance, member_cap) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, team.id().toString());
            statement.setString(2, team.name());
            statement.setString(3, team.tag());
            statement.setString(4, team.ownerId().toString());
            statement.setLong(5, team.createdAtMillis());
            statement.setString(6, team.description());
            statement.setString(7, team.motd());
            statement.setString(8, team.color().name());
            if (team.iconData().isEmpty()) {
                statement.setNull(9, Types.VARCHAR);
            } else {
                statement.setString(9, team.iconData());
            }
            statement.setInt(10, team.open() ? 1 : 0);
            statement.setInt(11, team.friendlyFire() ? 1 : 0);
            statement.setInt(12, team.allyFriendlyFire() ? 1 : 0);
            statement.setInt(13, team.level());
            statement.setDouble(14, team.balance());
            statement.setInt(15, team.memberCap());
            statement.executeUpdate();
        }
    }

    /** A new team with its owner, in one transaction. */
    public void insertTeam(Team team, TeamMember owner) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                saveTeam(connection, team);
                saveMember(connection, team.id(), owner);
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
        String id = teamId.toString();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (String sql : new String[] {
                        "DELETE FROM " + t.invites + " WHERE team_id = ?",
                        "DELETE FROM " + t.homes + " WHERE team_id = ?",
                        "DELETE FROM " + t.members + " WHERE team_id = ?",
                        "DELETE FROM " + t.chests + " WHERE team_id = ?",
                        "DELETE FROM " + t.bankLog + " WHERE team_id = ?",
                        "DELETE FROM " + t.relations + " WHERE team_id = ?",
                        "DELETE FROM " + t.relations + " WHERE other_id = ?",
                        "DELETE FROM " + t.teams + " WHERE id = ?"}) {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, id);
                        statement.executeUpdate();
                    }
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

    // ── Members ─────────────────────────────────────────────────────────────────────────────────────────────

    public void saveMember(UUID teamId, TeamMember member) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            saveMember(connection, teamId, member);
        }
    }

    private void saveMember(Connection connection, UUID teamId, TeamMember member) throws SQLException {
        String sql = "REPLACE INTO " + t.members + " (player_uuid, team_id, player_name, role, permissions, joined_at, last_seen, kills, deaths) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.playerId().toString());
            statement.setString(2, teamId.toString());
            statement.setString(3, member.name());
            statement.setString(4, member.role().name());
            statement.setString(5, TeamPermission.serialize(member.permissions()));
            statement.setLong(6, member.joinedAtMillis());
            statement.setLong(7, member.lastSeenMillis());
            statement.setInt(8, member.kills());
            statement.setInt(9, member.deaths());
            statement.executeUpdate();
        }
    }

    public void deleteMember(UUID playerId) throws SQLException {
        update("DELETE FROM " + t.members + " WHERE player_uuid = ?", playerId.toString());
    }

    /** Saves the team row and all its members in one transaction (ownership changes, admin edits). */
    public void saveTeamAndMembers(Team team) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                saveTeam(connection, team);
                for (TeamMember member : team.members()) {
                    saveMember(connection, team.id(), member);
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

    // ── Invites ─────────────────────────────────────────────────────────────────────────────────────────────

    public void saveInvite(TeamInvite invite) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "REPLACE INTO " + t.invites + " (team_id, player_uuid, invited_by, expires_at) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, invite.teamId().toString());
            statement.setString(2, invite.playerId().toString());
            statement.setString(3, invite.invitedBy().toString());
            statement.setLong(4, invite.expiresAtMillis());
            statement.executeUpdate();
        }
    }

    public void deleteInvite(UUID teamId, UUID playerId) throws SQLException {
        update("DELETE FROM " + t.invites + " WHERE team_id = ? AND player_uuid = ?", teamId.toString(), playerId.toString());
    }

    public void deleteInvitesFor(UUID playerId) throws SQLException {
        update("DELETE FROM " + t.invites + " WHERE player_uuid = ?", playerId.toString());
    }

    public void deleteExpiredInvites(long now) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM " + t.invites + " WHERE expires_at < ?")) {
            statement.setLong(1, now);
            statement.executeUpdate();
        }
    }

    // ── Homes ───────────────────────────────────────────────────────────────────────────────────────────────

    public void saveHome(TeamHome home) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + t.homes
                        + " (team_id, name, world, x, y, z, yaw, pitch, set_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, home.teamId().toString());
            statement.setString(2, home.name().toLowerCase(java.util.Locale.ROOT));
            statement.setString(3, home.world());
            statement.setDouble(4, home.x());
            statement.setDouble(5, home.y());
            statement.setDouble(6, home.z());
            statement.setFloat(7, home.yaw());
            statement.setFloat(8, home.pitch());
            if (home.setBy() == null) {
                statement.setNull(9, Types.VARCHAR);
            } else {
                statement.setString(9, home.setBy().toString());
            }
            statement.setLong(10, home.createdAtMillis());
            statement.executeUpdate();
        }
    }

    public void deleteHome(UUID teamId, String name) throws SQLException {
        update("DELETE FROM " + t.homes + " WHERE team_id = ? AND name = ?", teamId.toString(), name.toLowerCase(java.util.Locale.ROOT));
    }

    // ── Relations ───────────────────────────────────────────────────────────────────────────────────────────

    /** Saves how {@code team} sees {@code other} ({@code null} kind = neutral, the row is removed). */
    public void saveRelation(UUID team, UUID other, String kind) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + t.relations + " WHERE team_id = ? AND other_id = ?")) {
                delete.setString(1, team.toString());
                delete.setString(2, other.toString());
                delete.executeUpdate();
            }
            if (kind == null) {
                return;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + t.relations + " (team_id, other_id, kind, created_at) VALUES (?, ?, ?, ?)")) {
                insert.setString(1, team.toString());
                insert.setString(2, other.toString());
                insert.setString(3, kind);
                insert.setLong(4, System.currentTimeMillis());
                insert.executeUpdate();
            }
        }
    }

    // ── Chests ──────────────────────────────────────────────────────────────────────────────────────────────

    /** The stored chest contents, or an empty string when the team never used its chest. */
    public String loadChest(UUID teamId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT contents FROM " + t.chests + " WHERE team_id = ?")) {
            statement.setString(1, teamId.toString());
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getString(1) : "";
            }
        }
    }

    public void saveChest(UUID teamId, String contents) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "REPLACE INTO " + t.chests + " (team_id, contents, updated_at) VALUES (?, ?, ?)")) {
            statement.setString(1, teamId.toString());
            statement.setString(2, contents);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    // ── Bank history ────────────────────────────────────────────────────────────────────────────────────────

    public void insertBankEntry(BankEntry entry, int keep) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + t.bankLog
                    + " (team_id, player_uuid, player_name, kind, amount, balance_after, at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, entry.teamId().toString());
                if (entry.playerId() == null) {
                    statement.setNull(2, Types.VARCHAR);
                } else {
                    statement.setString(2, entry.playerId().toString());
                }
                statement.setString(3, entry.playerName() == null ? "" : entry.playerName());
                statement.setString(4, entry.type().name());
                statement.setDouble(5, entry.amount());
                statement.setDouble(6, entry.balanceAfter());
                statement.setLong(7, entry.atMillis());
                statement.executeUpdate();
            }
            if (keep > 0) {
                // Keep the newest `keep` lines. MariaDB cannot LIMIT inside IN (...), so find the cut-off id first.
                long cutoff = -1;
                try (PreparedStatement select = connection.prepareStatement("SELECT id FROM " + t.bankLog
                        + " WHERE team_id = ? ORDER BY id DESC LIMIT 1 OFFSET ?")) {
                    select.setString(1, entry.teamId().toString());
                    select.setInt(2, keep - 1);
                    try (ResultSet results = select.executeQuery()) {
                        if (results.next()) {
                            cutoff = results.getLong(1);
                        }
                    }
                }
                if (cutoff > 0) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM " + t.bankLog + " WHERE team_id = ? AND id < ?")) {
                        delete.setString(1, entry.teamId().toString());
                        delete.setLong(2, cutoff);
                        delete.executeUpdate();
                    }
                }
            }
        }
    }

    public List<BankEntry> loadBankHistory(UUID teamId, int limit) throws SQLException {
        List<BankEntry> entries = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT player_uuid, player_name, kind, amount, balance_after, at FROM "
                        + t.bankLog + " WHERE team_id = ? ORDER BY id DESC LIMIT ?")) {
            statement.setString(1, teamId.toString());
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String player = results.getString("player_uuid");
                    BankEntry.Type kind;
                    try {
                        kind = BankEntry.Type.valueOf(results.getString("kind"));
                    } catch (IllegalArgumentException ignored) {
                        kind = BankEntry.Type.ADMIN;
                    }
                    entries.add(new BankEntry(teamId, player == null ? null : uuid(player), results.getString("player_name"), kind,
                            results.getDouble("amount"), results.getDouble("balance_after"), results.getLong("at")));
                }
            }
        }
        return entries;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────────────────

    private void update(String sql, String... values) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 1, values[i]);
            }
            statement.executeUpdate();
        }
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value.trim());
    }
}
