package io.nightbeam.donutteams.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginSettings {

    private final JavaPlugin plugin;

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration yaml() {
        return plugin.getConfig();
    }

    public String sqliteFile() {
        return yaml().getString(SettingsSnapshot.SQLITE_FILE, "teams.db");
    }

    public int defaultMaxMembers() {
        return yaml().getInt(SettingsSnapshot.DEFAULT_MAX_MEMBERS, 8);
    }

    public int liteMaxMembers() {
        return yaml().getInt(SettingsSnapshot.LITE_MAX_MEMBERS, 8);
    }

    public int nameMin() {
        return yaml().getInt(SettingsSnapshot.NAME_MIN, 3);
    }

    public int nameMax() {
        return yaml().getInt(SettingsSnapshot.NAME_MAX, 16);
    }

    public int tagMin() {
        return yaml().getInt(SettingsSnapshot.TAG_MIN, 2);
    }

    public int tagMax() {
        return yaml().getInt(SettingsSnapshot.TAG_MAX, 6);
    }

    public int inviteExpireSeconds() {
        return yaml().getInt(SettingsSnapshot.INVITE_EXPIRE_SECONDS, 300);
    }

    public int homeWarmupSeconds() {
        return yaml().getInt(SettingsSnapshot.HOME_WARMUP_SECONDS, 3);
    }

    public boolean cancelHomeOnMove() {
        return yaml().getBoolean(SettingsSnapshot.HOME_CANCEL_ON_MOVE, true);
    }

    public boolean chatEnabled() {
        return yaml().getBoolean(SettingsSnapshot.CHAT_ENABLED, true);
    }

    public boolean defaultFriendlyFire() {
        return yaml().getBoolean(SettingsSnapshot.DEFAULT_FRIENDLY_FIRE, false);
    }

    public boolean metricsEnabled() {
        return yaml().getBoolean(SettingsSnapshot.METRICS_ENABLED, true);
    }

    public int bstatsId() {
        return yaml().getInt(SettingsSnapshot.BSTATS_ID, 0);
    }
}
