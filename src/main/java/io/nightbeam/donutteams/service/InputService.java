package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

/**
 * Text input for menu buttons. On Minecraft 1.21.7+ servers with dialogs enabled, a native input dialog opens;
 * otherwise the player types the answer in chat.
 */
public final class InputService {

    /** A native dialog implementation, present only on servers whose API has dialogs. */
    public interface Dialogs {
        void text(Player player, String title, String label, String initial, Consumer<String> answer, Runnable cancelled);

        void create(Player player, Consumer<String[]> answer);
    }

    private final DonutTeamsPlugin plugin;
    private final Dialogs dialogs;

    public InputService(DonutTeamsPlugin plugin, Dialogs dialogs) {
        this.plugin = plugin;
        this.dialogs = dialogs;
    }

    public boolean dialogsAvailable() {
        return dialogs != null;
    }

    private boolean useDialogs() {
        return dialogs != null && plugin.settings().dialogs;
    }

    /** Asks for one line of text. */
    public void text(Player player, String promptPath, Consumer<String> answer) {
        if (useDialogs()) {
            try {
                player.closeInventory();
                dialogs.text(player, plugin.messages().raw("dialog.text-title"), plugin.messages().raw(promptPath), "",
                        value -> plugin.scheduler().forPlayer(player, () -> answer.accept(value.trim())),
                        () -> plugin.messages().send(player, "prompt.cancelled"));
                return;
            } catch (Throwable error) {
                plugin.getLogger().warning("Dialogs failed, using chat prompts instead: " + error);
            }
        }
        plugin.chat().prompt(player, promptPath, answer);
    }

    /** Asks for a team name (and optional tag), then creates the team and opens its menu. */
    public void createTeam(Player player) {
        Consumer<String[]> create = parts -> {
            String name = parts.length > 0 ? parts[0] : "";
            String tag = parts.length > 1 && !parts[1].isBlank() ? parts[1] : null;
            plugin.teams().create(player, name, tag);
            if (plugin.teams().teamByPlayer(player.getUniqueId()) != null) {
                plugin.guis().openMain(player);
            }
        };
        if (useDialogs()) {
            try {
                player.closeInventory();
                dialogs.create(player, parts -> plugin.scheduler().forPlayer(player, () -> create.accept(parts)));
                return;
            } catch (Throwable error) {
                plugin.getLogger().warning("Dialogs failed, using chat prompts instead: " + error);
            }
        }
        plugin.chat().prompt(player, "prompt.create", answer -> create.accept(answer.trim().split("\\s+")));
    }
}
