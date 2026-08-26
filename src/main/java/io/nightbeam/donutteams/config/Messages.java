package io.nightbeam.donutteams.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final JavaPlugin plugin;
    private FileConfiguration yaml;
    private String prefixRaw = "<dark_gray>[<gold><bold>DonutTeams</bold></gold>]</dark_gray> ";

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration yaml) {
        this.yaml = yaml == null ? new YamlConfiguration() : yaml;
        String loaded = this.yaml.getString("prefix");
        this.prefixRaw = loaded == null || loaded.isBlank()
                ? "<dark_gray>[<gold><bold>DonutTeams</bold></gold>]</dark_gray> "
                : loaded;
    }

    public String raw(String path, String def) {
        if (yaml == null) {
            return def;
        }
        String value = yaml.getString(path);
        return value == null || value.isEmpty() ? def : value;
    }

    public Component component(String path, String def, String... keyValues) {
        return deserialize(raw(path, def), keyValues);
    }

    public Component prefixed(String path, String def, String... keyValues) {
        return deserialize(prefixRaw + raw(path, def), keyValues);
    }

    public List<Component> list(String path, String... keyValues) {
        List<String> lines = yaml == null ? List.of() : yaml.getStringList(path);
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(deserialize(line, keyValues));
        }
        return out;
    }

    public void send(CommandSender sender, String path, String def, String... keyValues) {
        sender.sendMessage(prefixed(path, def, keyValues));
    }

    public Component deserialize(String template, String... keyValues) {
        if (template == null || template.isEmpty()) {
            return Component.empty();
        }
        Map<String, String> placeholders = toMap(keyValues);
        TagResolver.Builder builder = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            builder.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue() == null ? "" : entry.getValue()));
        }
        try {
            return MINI.deserialize(template, builder.build());
        } catch (Exception ignored) {
            return Component.text(template);
        }
    }

    public String plain(Component component) {
        return PLAIN.serialize(component);
    }

    public MiniMessage mini() {
        return MINI;
    }

    private static Map<String, String> toMap(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        if (keyValues == null) {
            return map;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1] == null ? "" : keyValues[i + 1]);
        }
        return map;
    }
}
