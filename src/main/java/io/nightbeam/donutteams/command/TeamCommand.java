package io.nightbeam.donutteams.command;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.gui.GuiManager;
import io.nightbeam.donutteams.service.ChatService;
import io.nightbeam.donutteams.service.HomeWarmupService;
import io.nightbeam.donutteams.service.TeamService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TeamCommand implements TabExecutor {

    private static final List<String> SUBS = List.of(
            "create", "disband", "invite", "join", "leave", "kick", "info",
            "chat", "home", "sethome", "delhome", "pvp", "transfer");

    private final Messages messages;
    private final TeamService teams;
    private final ChatService chat;
    private final HomeWarmupService homes;
    private final GuiManager guis;

    public TeamCommand(DonutTeamsPlugin plugin, TeamService teams, ChatService chat, HomeWarmupService homes, GuiManager guis) {
        this.messages = plugin.messages();
        this.teams = teams;
        this.chat = chat;
        this.homes = homes;
        this.guis = guis;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only", "<red>Players only.");
            return true;
        }
        if (args.length == 0) {
            guis.openMain(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    messages.send(player, "general.usage", "<gray>Usage", "usage", messages.raw("create.usage", "/team create <name> [tag]"));
                    return true;
                }
                String tag = args.length >= 3 ? args[2] : args[1];
                teams.create(player, args[1], tag);
            }
            case "disband" -> teams.disband(player, args.length >= 2 && args[1].equalsIgnoreCase("confirm"));
            case "invite" -> {
                if (args.length < 2) {
                    messages.send(player, "general.usage", "<gray>Usage", "usage", messages.raw("invite.usage", "/team invite <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    messages.send(player, "general.player-not-found", "<red>Not found.", "player", args[1]);
                    return true;
                }
                teams.invite(player, target);
            }
            case "join" -> {
                if (args.length < 2) {
                    messages.send(player, "general.usage", "<gray>Usage", "usage", messages.raw("join.usage", "/team join <team>"));
                    return true;
                }
                teams.join(player, args[1]);
            }
            case "leave" -> teams.leave(player);
            case "kick" -> {
                if (args.length < 2) {
                    messages.send(player, "general.usage", "<gray>Usage", "usage", messages.raw("kick.usage", "/team kick <player>"));
                    return true;
                }
                teams.kick(player, args[1]);
            }
            case "info" -> teams.info(player, args.length >= 2 ? args[1] : null);
            case "chat" -> {
                String message = args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : null;
                chat.toggleOrSend(player, message);
            }
            case "home" -> homes.start(player);
            case "sethome" -> teams.setHome(player);
            case "delhome" -> teams.deleteHome(player);
            case "pvp" -> teams.togglePvp(player);
            case "transfer" -> {
                if (args.length < 2) {
                    messages.send(player, "general.usage", "<gray>Usage", "usage", messages.raw("transfer.usage", "/team transfer <player>"));
                    return true;
                }
                teams.transfer(player, args[1]);
            }
            default -> messages.send(player, "general.unknown-subcommand", "<red>Unknown.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return prefix(SUBS, args[0]);
        }
        if (args.length == 2 && List.of("invite", "kick", "transfer").contains(args[0].toLowerCase(Locale.ROOT))) {
            return prefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            if (sender instanceof Player player) {
                return prefix(teams.cache().invitesFor(player.getUniqueId()).stream()
                        .map(invite -> {
                            var team = teams.teamById(invite.teamId());
                            return team == null ? null : team.name();
                        })
                        .filter(name -> name != null)
                        .toList(), args[1]);
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("disband")) {
            return prefix(List.of("confirm"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return prefix(teams.cache().teams().stream().map(team -> team.name()).toList(), args[1]);
        }
        return List.of();
    }

    private static List<String> prefix(List<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(value);
            }
        }
        return out;
    }
}
