package io.nightbeam.donutteams.command;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /tc and /ac: team chat and ally chat, as a message or a toggle. */
public final class ChatCommand implements TabExecutor {

    private final DonutTeamsPlugin plugin;
    private final boolean ally;

    public ChatCommand(DonutTeamsPlugin plugin, boolean ally) {
        this.plugin = plugin;
        this.ally = ally;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "error.players-only");
            return true;
        }
        String message = args.length == 0 ? null : String.join(" ", args);
        if (ally) {
            plugin.chat().allyChat(player, message);
        } else {
            plugin.chat().teamChat(player, message);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
