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
        return yaml().getString("storage.sqlite.file", "teams.db");
    }

    public int defaultMaxMembers() {
        return yaml().getInt("teams.default-max-members", 8);
    }

    public int liteMaxMembers() {
        return yaml().getInt("teams.lite-max-members", 8);
    }

    public int nameMin() {
        return yaml().getInt("teams.name-min", 3);
    }

    public int nameMax() {
        return yaml().getInt("teams.name-max", 16);
    }

    public int tagMin() {
        return yaml().getInt("teams.tag-min", 2);
    }

    public int tagMax() {
        return yaml().getInt("teams.tag-max", 6);
    }

    public int inviteExpireSeconds() {
        return yaml().getInt("teams.invite-expire-seconds", 300);
    }

    public int homeWarmupSeconds() {
        return yaml().getInt("home.warmup-seconds", 3);
    }

    public boolean cancelHomeOnMove() {
        return yaml().getBoolean("home.cancel-on-move", true);
    }

    public boolean chatEnabled() {
        return yaml().getBoolean("chat.enabled", true);
    }

    public boolean defaultFriendlyFire() {
        return yaml().getBoolean("pvp.default-friendly-fire", false);
    }

    public boolean metricsEnabled() {
        return yaml().getBoolean("metrics.enabled", true);
    }

    public int bstatsId() {
        return yaml().getInt("metrics.bstats-id", 0);
    }
}
