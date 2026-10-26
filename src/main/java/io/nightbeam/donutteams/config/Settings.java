package io.nightbeam.donutteams.config;

import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.storage.DatabaseType;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

/** An immutable view of config.yml. A reload builds a new one. */
public final class Settings {

    public final DatabaseType storageType;
    public final String sqliteFile;
    public final String dbHost;
    public final int dbPort;
    public final String dbName;
    public final String dbUser;
    public final String dbPassword;
    public final String dbParameters;
    public final int dbPoolSize;
    public final String tablePrefix;

    public final int nameMin;
    public final int nameMax;
    public final int tagMin;
    public final int tagMax;
    public final List<String> blockedNames;
    public final double createCost;
    public final boolean defaultOpen;
    public final int maxMembersCap;
    public final int inviteExpireSeconds;
    public final int requestExpireSeconds;
    public final List<String> disabledHomeWorlds;

    public final Set<TeamPermission> officerPermissions;
    public final Set<TeamPermission> memberPermissions;

    public final boolean levelsEnabled;
    public final LevelTable levels;

    public final int homeWarmupSeconds;
    public final boolean homeCancelOnMove;
    public final boolean homeCancelOnDamage;
    public final int homeCooldownSeconds;
    public final boolean homeTitle;
    public final boolean homeParticles;

    public final boolean chatEnabled;
    public final boolean allyChat;
    public final String quickPrefix;
    public final String allyQuickPrefix;
    public final boolean chatLogToConsole;

    public final boolean defaultFriendlyFire;
    public final boolean defaultAllyFriendlyFire;

    public final boolean alliesEnabled;
    public final boolean enemiesEnabled;
    public final int maxAllies;
    public final int allyRequestExpireSeconds;

    public final boolean bankEnabled;
    public final double bankMaxBalance;
    public final int bankHistorySize;

    public final boolean chestEnabled;
    public final int chestAutosaveSeconds;

    public final boolean statsEnabled;

    public final int leaderboardRefreshSeconds;
    public final int boardRefreshSeconds;
    public final int boardRows;

    public final boolean notifyOnline;
    public final boolean notifyOffline;
    public final boolean motdOnJoin;

    public final boolean dialogs;

    public final boolean updateCheck;
    public final boolean metrics;

    private Settings(ConfigurationSection c) {
        storageType = DatabaseType.parse(c.getString("storage.type", "SQLITE"));
        sqliteFile = text(c.getString("storage.sqlite.file"), "teams.db");
        dbHost = text(c.getString("storage.mariadb.host"), "localhost");
        dbPort = c.getInt("storage.mariadb.port", 3306);
        dbName = text(c.getString("storage.mariadb.database"), "donutteams");
        dbUser = text(c.getString("storage.mariadb.username"), "donutteams");
        dbPassword = c.getString("storage.mariadb.password", "");
        dbParameters = c.getString("storage.mariadb.parameters", "");
        dbPoolSize = Math.max(2, c.getInt("storage.mariadb.pool-size", 6));
        String prefix = c.getString("storage.table-prefix", "");
        tablePrefix = prefix == null ? "" : prefix.replaceAll("[^A-Za-z0-9_]", "");

        nameMin = Math.max(1, c.getInt("teams.name-min", 3));
        nameMax = Math.max(nameMin, c.getInt("teams.name-max", 16));
        tagMin = Math.max(1, c.getInt("teams.tag-min", 2));
        tagMax = Math.max(tagMin, c.getInt("teams.tag-max", 6));
        blockedNames = List.copyOf(c.getStringList("teams.blocked-names"));
        createCost = Math.max(0, c.getDouble("teams.create-cost", 0));
        defaultOpen = c.getBoolean("teams.default-open", false);
        maxMembersCap = Math.max(1, c.getInt("teams.max-members-cap", 50));
        inviteExpireSeconds = Math.max(10, c.getInt("teams.invite-expire-seconds", 300));
        requestExpireSeconds = Math.max(10, c.getInt("teams.request-expire-seconds", 300));
        disabledHomeWorlds = c.getStringList("teams.disabled-home-worlds").stream().map(w -> w.toLowerCase(Locale.ROOT)).toList();

        officerPermissions = permissions(c, "roles.officer.permissions", EnumSet.of(TeamPermission.INVITE, TeamPermission.KICK,
                TeamPermission.HOME, TeamPermission.SETHOME, TeamPermission.SPEAK, TeamPermission.PVP, TeamPermission.BANK_DEPOSIT,
                TeamPermission.BANK_WITHDRAW, TeamPermission.CHEST, TeamPermission.RELATIONS, TeamPermission.SETTINGS));
        memberPermissions = permissions(c, "roles.member.permissions", EnumSet.of(TeamPermission.HOME, TeamPermission.SPEAK,
                TeamPermission.BANK_DEPOSIT, TeamPermission.CHEST));

        levelsEnabled = c.getBoolean("levels.enabled", true);
        levels = LevelTable.from(c.getConfigurationSection("levels.list"));

        homeWarmupSeconds = Math.max(0, c.getInt("homes.warmup-seconds", 3));
        homeCancelOnMove = c.getBoolean("homes.cancel-on-move", true);
        homeCancelOnDamage = c.getBoolean("homes.cancel-on-damage", true);
        homeCooldownSeconds = Math.max(0, c.getInt("homes.cooldown-seconds", 0));
        homeTitle = c.getBoolean("homes.title", true);
        homeParticles = c.getBoolean("homes.particles", true);

        chatEnabled = c.getBoolean("chat.enabled", true);
        allyChat = c.getBoolean("chat.ally-chat", true);
        quickPrefix = c.getString("chat.quick-prefix", "");
        allyQuickPrefix = c.getString("chat.ally-quick-prefix", "");
        chatLogToConsole = c.getBoolean("chat.log-to-console", true);

        defaultFriendlyFire = c.getBoolean("pvp.default-friendly-fire", false);
        defaultAllyFriendlyFire = c.getBoolean("pvp.default-ally-friendly-fire", false);

        alliesEnabled = c.getBoolean("relations.allies", true);
        enemiesEnabled = c.getBoolean("relations.enemies", true);
        maxAllies = Math.max(0, c.getInt("relations.max-allies", 5));
        allyRequestExpireSeconds = Math.max(10, c.getInt("relations.request-expire-seconds", 300));

        bankEnabled = c.getBoolean("bank.enabled", true);
        bankMaxBalance = Math.max(0, c.getDouble("bank.max-balance", 0));
        bankHistorySize = Math.max(0, c.getInt("bank.history-size", 45));

        chestEnabled = c.getBoolean("chest.enabled", true);
        chestAutosaveSeconds = Math.max(10, c.getInt("chest.autosave-seconds", 60));

        statsEnabled = c.getBoolean("stats.enabled", true);

        leaderboardRefreshSeconds = Math.max(5, c.getInt("leaderboard.refresh-seconds", 30));
        boardRefreshSeconds = Math.max(5, c.getInt("leaderboard.board-refresh-seconds", 30));
        boardRows = Math.max(1, Math.min(20, c.getInt("leaderboard.board-rows", 10)));

        notifyOnline = c.getBoolean("notifications.member-online", true);
        notifyOffline = c.getBoolean("notifications.member-offline", true);
        motdOnJoin = c.getBoolean("notifications.motd-on-join", true);

        dialogs = c.getBoolean("menus.dialogs", true);

        updateCheck = c.getBoolean("update-check.enabled", true);
        metrics = c.getBoolean("metrics.enabled", true);
    }

    public static Settings from(ConfigurationSection config) {
        return new Settings(config == null ? new MemoryConfiguration() : config);
    }

    public static Settings defaults() {
        return from(new MemoryConfiguration());
    }

    public Set<TeamPermission> defaultPermissions(TeamRole role) {
        return switch (role) {
            case OWNER -> TeamPermission.all();
            case OFFICER -> officerPermissions;
            case MEMBER -> memberPermissions;
        };
    }

    private static Set<TeamPermission> permissions(ConfigurationSection c, String path, EnumSet<TeamPermission> fallback) {
        if (!c.isList(path)) {
            return fallback;
        }
        return TeamPermission.parseAll(c.getStringList(path));
    }

    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
