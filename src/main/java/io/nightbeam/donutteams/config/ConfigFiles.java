package io.nightbeam.donutteams.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Keeps config.yml, messages.yml and menus.yml current. 1.x files are upgraded on the first 2.0 start (the old ones
 * are kept as config-1.x.yml and messages-1.x.yml); afterwards, keys added by an update are merged in without
 * touching anything an admin changed.
 */
public final class ConfigFiles {

    /** Sections whose entries an admin may delete on purpose: only added back when the whole section is gone. */
    private static final Set<String> KEEP_AS_IS = Set.of("levels.list", "roles");

    private ConfigFiles() {
    }

    /** Returns notes for the console about what changed. */
    public static List<String> prepare(JavaPlugin plugin) {
        List<String> notes = new ArrayList<>();
        File folder = plugin.getDataFolder();
        folder.mkdirs();
        File config = new File(folder, "config.yml");
        if (config.exists()) {
            YamlConfiguration current = YamlConfiguration.loadConfiguration(config);
            if (!current.contains("config-version")) {
                upgradeConfig(plugin, config, current);
                notes.add("Upgraded config.yml from 1.x (the old file is config-1.x.yml).");
            } else {
                merge(plugin, "config.yml", false, notes);
            }
        } else {
            plugin.saveResource("config.yml", false);
        }
        File messages = new File(folder, "messages.yml");
        if (messages.exists()) {
            YamlConfiguration current = YamlConfiguration.loadConfiguration(messages);
            if (!current.contains("messages-version")) {
                backup(messages, "messages-1.x.yml");
                plugin.saveResource("messages.yml", true);
                notes.add("messages.yml was replaced with the 2.0 version (your 1.x file is messages-1.x.yml).");
            } else {
                merge(plugin, "messages.yml", false, notes);
            }
        } else {
            plugin.saveResource("messages.yml", false);
        }
        if (new File(folder, "menus.yml").exists()) {
            merge(plugin, "menus.yml", true, notes);
        } else {
            plugin.saveResource("menus.yml", false);
        }
        return notes;
    }

    /** 1.x keys moved to their 2.0 places; everything else takes the 2.0 default. */
    private static void upgradeConfig(JavaPlugin plugin, File file, YamlConfiguration old) {
        backup(file, "config-1.x.yml");
        plugin.saveResource("config.yml", true);
        YamlConfiguration next = YamlConfiguration.loadConfiguration(file);
        copy(old, next, "storage.sqlite.file", "storage.sqlite.file");
        copy(old, next, "teams.name-min", "teams.name-min");
        copy(old, next, "teams.name-max", "teams.name-max");
        copy(old, next, "teams.tag-min", "teams.tag-min");
        copy(old, next, "teams.tag-max", "teams.tag-max");
        copy(old, next, "teams.invite-expire-seconds", "teams.invite-expire-seconds");
        copy(old, next, "teams.default-max-members", "levels.list.1.max-members");
        copy(old, next, "home.warmup-seconds", "homes.warmup-seconds");
        copy(old, next, "home.cancel-on-move", "homes.cancel-on-move");
        copy(old, next, "chat.enabled", "chat.enabled");
        copy(old, next, "pvp.default-friendly-fire", "pvp.default-friendly-fire");
        copy(old, next, "metrics.enabled", "metrics.enabled");
        try {
            next.save(file);
        } catch (IOException error) {
            plugin.getLogger().log(Level.WARNING, "Could not save the upgraded config.yml", error);
        }
    }

    private static void copy(ConfigurationSection from, ConfigurationSection to, String fromPath, String toPath) {
        if (from.contains(fromPath)) {
            to.set(toPath, from.get(fromPath));
        }
    }

    /** Adds keys the shipped file has and the admin's lacks. With {@code topLevelOnly}, only whole sections. */
    static void merge(JavaPlugin plugin, String name, boolean topLevelOnly, List<String> notes) {
        File file = new File(plugin.getDataFolder(), name);
        YamlConfiguration defaults = shipped(plugin, name);
        if (defaults == null) {
            return;
        }
        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
        int before = current.getKeys(true).size();
        if (topLevelOnly) {
            for (String key : defaults.getKeys(false)) {
                if (!current.contains(key)) {
                    current.set(key, defaults.get(key));
                }
            }
        } else {
            mergeMissing(current, defaults, "");
        }
        if (current.getKeys(true).size() != before) {
            try {
                current.save(file);
                notes.add("Added new settings to " + name + ".");
            } catch (IOException error) {
                plugin.getLogger().log(Level.WARNING, "Could not save " + name, error);
            }
        }
    }

    static void mergeMissing(ConfigurationSection target, ConfigurationSection defaults, String path) {
        for (String key : defaults.getKeys(false)) {
            String full = path.isEmpty() ? key : path + "." + key;
            if (!target.contains(key, true)) {
                target.set(key, defaults.get(key));
            } else if (!KEEP_AS_IS.contains(full) && target.isConfigurationSection(key) && defaults.isConfigurationSection(key)) {
                mergeMissing(target.getConfigurationSection(key), defaults.getConfigurationSection(key), full);
            }
        }
    }

    public static YamlConfiguration shipped(JavaPlugin plugin, String name) {
        try (InputStream stream = plugin.getResource(name)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException error) {
            return null;
        }
    }

    /** Loads a data-folder file with the shipped copy as defaults (so a missing key never breaks anything). */
    public static YamlConfiguration load(JavaPlugin plugin, String name) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
        YamlConfiguration defaults = shipped(plugin, name);
        if (defaults != null) {
            yaml.setDefaults(defaults);
        }
        return yaml;
    }

    private static void backup(File file, String backupName) {
        try {
            Files.copy(file.toPath(), new File(file.getParentFile(), backupName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // The upgrade still goes ahead; the admin is told the old values were moved.
        }
    }
}
