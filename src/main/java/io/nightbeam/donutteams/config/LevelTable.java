package io.nightbeam.donutteams.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;

/** The levels a team can reach, from config.yml {@code levels.list}. Always has at least level 1. */
public final class LevelTable {

    public record Level(int level, double cost, int maxMembers, int maxHomes, int chestRows) {
    }

    private final List<Level> levels;

    public LevelTable(List<Level> levels) {
        TreeMap<Integer, Level> sorted = new TreeMap<>();
        for (Level level : levels) {
            if (level.level() >= 1) {
                sorted.put(level.level(), level);
            }
        }
        if (!sorted.containsKey(1)) {
            sorted.put(1, new Level(1, 0, 8, 1, 1));
        }
        // Levels must be consecutive: stop at the first gap.
        List<Level> out = new ArrayList<>();
        int expected = 1;
        for (Map.Entry<Integer, Level> entry : sorted.entrySet()) {
            if (entry.getKey() != expected) {
                break;
            }
            out.add(entry.getValue());
            expected++;
        }
        this.levels = Collections.unmodifiableList(out);
    }

    public static LevelTable from(ConfigurationSection section) {
        List<Level> levels = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                int number;
                try {
                    number = Integer.parseInt(key.trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (entry == null) {
                    continue;
                }
                levels.add(new Level(number,
                        Math.max(0, entry.getDouble("cost", 0)),
                        Math.max(1, entry.getInt("max-members", 8)),
                        Math.max(0, entry.getInt("max-homes", 1)),
                        Math.max(1, Math.min(6, entry.getInt("chest-rows", 1)))));
            }
        }
        return new LevelTable(levels);
    }

    public List<Level> all() {
        return levels;
    }

    public int max() {
        return levels.size();
    }

    /** The given level, clamped to the table. */
    public Level get(int level) {
        int index = Math.max(1, Math.min(level, levels.size())) - 1;
        return levels.get(index);
    }

    /** The level after this one, or null at the top. */
    public Level next(int level) {
        return level >= levels.size() ? null : levels.get(Math.max(0, level));
    }
}
