package io.nightbeam.donutteams.board;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.service.TopService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

/**
 * Top-teams boards standing in the world: one text display per board, no hologram plugin needed. Displays are not
 * saved with their chunk; each refresh runs on the region thread that owns the board (Folia-safe), and leftovers
 * carrying the board's tag are removed before a board is spawned again.
 */
public final class BoardService {

    public record Board(String name, String world, double x, double y, double z, TopService.Stat stat) {
    }

    private final DonutTeamsPlugin plugin;
    private final FoliaScheduler scheduler;
    private final File file;
    private final NamespacedKey key;
    private final Map<String, Board> boards = new ConcurrentHashMap<>();
    private final Map<String, UUID> entities = new ConcurrentHashMap<>();
    private volatile FoliaScheduler.Task task = FoliaScheduler.Task.NONE;

    public BoardService(DonutTeamsPlugin plugin, FoliaScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.file = new File(plugin.getDataFolder(), "boards.yml");
        this.key = new NamespacedKey(plugin, "board");
        load();
    }

    private void load() {
        boards.clear();
        if (!file.exists()) {
            return;
        }
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("boards");
        if (section == null) {
            return;
        }
        for (String name : section.getKeys(false)) {
            ConfigurationSection board = section.getConfigurationSection(name);
            if (board == null || board.getString("world") == null) {
                continue;
            }
            boards.put(name.toLowerCase(Locale.ROOT), new Board(name, board.getString("world"), board.getDouble("x"),
                    board.getDouble("y"), board.getDouble("z"), TopService.Stat.parse(board.getString("stat"), TopService.Stat.KILLS)));
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Board board : boards.values()) {
            String path = "boards." + board.name();
            yaml.set(path + ".world", board.world());
            yaml.set(path + ".x", board.x());
            yaml.set(path + ".y", board.y());
            yaml.set(path + ".z", board.z());
            yaml.set(path + ".stat", board.stat().key());
        }
        try {
            yaml.save(file);
        } catch (IOException error) {
            plugin.getLogger().log(Level.WARNING, "Could not save boards.yml", error);
        }
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        boards.values().forEach(board -> names.add(board.name()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean create(String name, TopService.Stat stat, Location at) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}") || at.getWorld() == null || boards.containsKey(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        boards.put(name.toLowerCase(Locale.ROOT), new Board(name, at.getWorld().getName(), at.getX(), at.getY(), at.getZ(), stat));
        save();
        refresh();
        return true;
    }

    public boolean delete(String name) {
        Board removed = boards.remove(name == null ? "" : name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        save();
        UUID id = entities.remove(removed.name().toLowerCase(Locale.ROOT));
        Location location = location(removed);
        if (location != null) {
            scheduler.runAtLocation(location, () -> removeAt(location, removed.name(), id));
        }
        return true;
    }

    public void start(int seconds) {
        stop();
        task = scheduler.runAsyncTimer(this::refresh, 3, Math.max(5, seconds));
    }

    public void stop() {
        task.cancel();
        task = FoliaScheduler.Task.NONE;
    }

    public void refresh() {
        for (Board board : boards.values()) {
            Location location = location(board);
            if (location != null) {
                scheduler.runAtLocation(location, () -> update(board, location));
            }
        }
    }

    private Location location(Board board) {
        World world = Bukkit.getWorld(board.world());
        return world == null ? null : new Location(world, board.x(), board.y(), board.z());
    }

    private void update(Board board, Location location) {
        if (!location.isChunkLoaded()) {
            return;
        }
        String id = board.name().toLowerCase(Locale.ROOT);
        UUID known = entities.get(id);
        Entity entity = known == null ? null : location.getWorld().getEntity(known);
        TextDisplay display = entity instanceof TextDisplay existing && existing.isValid() ? existing : null;
        if (display == null) {
            removeAt(location, board.name(), null);
            display = location.getWorld().spawn(location, TextDisplay.class, spawned -> {
                spawned.setPersistent(false);
                spawned.setBillboard(Display.Billboard.CENTER);
                spawned.setShadowed(true);
                spawned.setLineWidth(280);
                spawned.setBackgroundColor(Color.fromARGB(170, 12, 22, 20));
                spawned.getPersistentDataContainer().set(key, PersistentDataType.STRING, board.name());
            });
            entities.put(id, display.getUniqueId());
        }
        float scale = plugin.settings().boardScale;
        Transformation now = display.getTransformation();
        if (now.getScale().x() != scale) {
            display.setTransformation(new Transformation(now.getTranslation(), now.getLeftRotation(),
                    new Vector3f(scale, scale, scale), now.getRightRotation()));
        }
        display.text(render(board.stat()));
    }

    Component render(TopService.Stat stat) {
        var messages = plugin.messages();
        var format = plugin.teams().format();
        net.kyori.adventure.text.TextComponent.Builder lines = Component.text();
        lines.append(messages.component("board.title", "stat_name", messages.raw("stats." + stat.key())));
        List<Team> top = plugin.top().top(stat);
        int rows = plugin.settings().boardRows;
        if (top.isEmpty()) {
            lines.append(Component.newline()).append(messages.component("board.empty"));
        }
        for (int i = 0; i < Math.min(rows, top.size()); i++) {
            Team team = top.get(i);
            String value = switch (stat) {
                case KILLS -> String.valueOf(team.kills());
                case BALANCE -> format.money(team.balance());
                case LEVEL -> String.valueOf(team.level());
                case MEMBERS -> String.valueOf(team.size());
            };
            lines.append(Component.newline()).append(messages.component(i < 3 ? "board.entry-top" : "board.entry",
                    format.team(team, "rank", String.valueOf(i + 1), "value", value)));
        }
        lines.append(Component.newline()).append(messages.component("board.footer"));
        return lines.build();
    }

    private void removeAt(Location location, String name, UUID known) {
        if (!location.isChunkLoaded()) {
            return;
        }
        if (known != null) {
            Entity entity = location.getWorld().getEntity(known);
            if (entity != null) {
                entity.remove();
            }
        }
        for (TextDisplay nearby : location.getWorld().getNearbyEntitiesByType(TextDisplay.class, location, 2.0)) {
            if (name.equals(nearby.getPersistentDataContainer().get(key, PersistentDataType.STRING))) {
                nearby.remove();
            }
        }
    }

    public void shutdown() {
        stop();
        entities.clear();
    }
}
