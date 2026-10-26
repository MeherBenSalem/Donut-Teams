package io.nightbeam.donutteams.command;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.service.TopService;
import io.nightbeam.donutteams.util.Amounts;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** /donutteams: staff tools. Works from the console except where a player must stand somewhere. */
public final class AdminCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("reload", "info", "disband", "join", "kick", "setowner", "rename", "bank",
            "level", "chest", "spy", "board", "purge", "version");

    private final DonutTeamsPlugin plugin;
    private final Messages messages;
    private final TeamService teams;

    public AdminCommand(DonutTeamsPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.teams = plugin.teams();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("donutteams.admin")) {
            messages.send(sender, "error.no-permission");
            return true;
        }
        if (args.length == 0) {
            messages.send(sender, "admin.usage");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                messages.send(sender, "admin.reload");
            }
            case "version", "about" -> version(sender);
            case "info" -> {
                if (args.length < 2) {
                    version(sender);
                    plugin.report(sender);
                } else {
                    Team team = team(sender, args[1]);
                    if (team != null) {
                        plugin.teamCommand().info(sender, team, null);
                    }
                }
            }
            case "disband" -> {
                Team team = args.length >= 2 ? team(sender, args[1]) : usage(sender, "/donutteams disband <team>");
                if (team != null) {
                    teams.disbandTeam(sender, team, true);
                }
            }
            case "join" -> {
                if (args.length < 3) {
                    usage(sender, "/donutteams join <player> <team>");
                    return true;
                }
                Player player = Bukkit.getPlayerExact(args[1]);
                Team team = team(sender, args[2]);
                if (player == null) {
                    messages.send(sender, "error.player-not-found", "player", args[1]);
                } else if (team != null) {
                    teams.adminJoin(sender, player, team);
                }
            }
            case "kick" -> {
                if (args.length < 2) {
                    usage(sender, "/donutteams kick <player>");
                } else {
                    teams.adminKick(sender, args[1]);
                }
            }
            case "setowner" -> {
                if (args.length < 3) {
                    usage(sender, "/donutteams setowner <team> <player>");
                    return true;
                }
                Team team = team(sender, args[1]);
                if (team != null) {
                    teams.adminSetOwner(sender, team, args[2]);
                }
            }
            case "rename" -> {
                if (args.length < 3) {
                    usage(sender, "/donutteams rename <team> <name> [tag]");
                    return true;
                }
                Team team = team(sender, args[1]);
                if (team != null) {
                    teams.renameTeam(sender, team, args[2], args.length >= 4 ? args[3] : team.tag());
                }
            }
            case "bank" -> {
                if (args.length < 4) {
                    usage(sender, "/donutteams bank <team> <set|give|take> <amount>");
                    return true;
                }
                Team team = team(sender, args[1]);
                String action = args[2].toLowerCase(Locale.ROOT);
                double amount = args[3].equals("0") ? 0 : Amounts.parse(args[3]);
                if (team == null) {
                    return true;
                }
                if (amount < 0 || !List.of("set", "give", "take").contains(action)) {
                    usage(sender, "/donutteams bank <team> <set|give|take> <amount>");
                    return true;
                }
                plugin.bank().adminBalance(sender, team, action, amount);
            }
            case "level" -> {
                if (args.length < 3) {
                    usage(sender, "/donutteams level <team> <level>");
                    return true;
                }
                Team team = team(sender, args[1]);
                try {
                    if (team != null) {
                        plugin.bank().adminLevel(sender, team, Integer.parseInt(args[2]));
                    }
                } catch (NumberFormatException ignored) {
                    usage(sender, "/donutteams level <team> <level>");
                }
            }
            case "chest" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "error.players-only");
                    return true;
                }
                Team team = args.length >= 2 ? team(sender, args[1]) : usage(sender, "/donutteams chest <team>");
                if (team != null) {
                    plugin.chests().open(player, team, true);
                }
            }
            case "spy" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "error.players-only");
                    return true;
                }
                if (!player.hasPermission("donutteams.admin.spy")) {
                    messages.send(sender, "error.no-permission");
                    return true;
                }
                messages.send(player, plugin.chat().toggleSpy(player) ? "chat.spy-on" : "chat.spy-off");
            }
            case "board" -> board(sender, args);
            case "purge" -> purge(sender, args);
            default -> messages.send(sender, "admin.usage");
        }
        return true;
    }

    private void board(CommandSender sender, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (action) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "error.players-only");
                    return;
                }
                if (args.length < 3) {
                    usage(sender, "/donutteams board create <name> [kills|balance|level|members]");
                    return;
                }
                TopService.Stat stat = TopService.Stat.parse(args.length >= 4 ? args[3] : null, TopService.Stat.KILLS);
                if (plugin.boards().create(args[2], stat, player.getLocation().add(0, 2.2, 0))) {
                    messages.send(sender, "admin.board-created", "board", args[2], "stat", messages.raw("stats." + stat.key()));
                } else {
                    messages.send(sender, "admin.board-exists", "board", args[2]);
                }
            }
            case "delete", "remove" -> {
                if (args.length < 3) {
                    usage(sender, "/donutteams board delete <name>");
                } else if (plugin.boards().delete(args[2])) {
                    messages.send(sender, "admin.board-deleted", "board", args[2]);
                } else {
                    messages.send(sender, "admin.board-missing", "board", args[2]);
                }
            }
            default -> messages.send(sender, "admin.board-list", "boards", String.join(", ", plugin.boards().names()));
        }
    }

    /** Disbands teams whose members have all been away for the given number of days. */
    private void purge(CommandSender sender, String[] args) {
        int days;
        try {
            days = Integer.parseInt(args.length >= 2 ? args[1] : "");
        } catch (NumberFormatException ignored) {
            usage(sender, "/donutteams purge <days> [confirm]");
            return;
        }
        long cutoff = System.currentTimeMillis() - Math.max(1, days) * 86_400_000L;
        java.util.List<Team> inactive = new java.util.ArrayList<>();
        for (Team team : teams.cache().teams()) {
            boolean active = team.members().stream().anyMatch(member -> member.lastSeenMillis() >= cutoff
                    || Bukkit.getPlayer(member.playerId()) != null);
            if (!active) {
                inactive.add(team);
            }
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            messages.send(sender, "admin.purge-preview", "count", inactive.size(), "days", days,
                    "teams", inactive.stream().limit(15).map(Team::name).collect(java.util.stream.Collectors.joining(", ")));
            return;
        }
        for (Team team : inactive) {
            teams.disbandTeam(sender, team, true);
        }
        messages.send(sender, "admin.purged", "count", inactive.size(), "days", days);
    }

    private void version(CommandSender sender) {
        messages.send(sender, "admin.version", "version", plugin.getPluginMeta().getVersion(), "platform", plugin.platform(),
                "storage", plugin.storageName(), "economy", plugin.economy().available() ? plugin.economy().name() : "none");
    }

    private Team team(CommandSender sender, String name) {
        Team team = teams.teamByName(name);
        if (team == null) {
            messages.send(sender, "error.team-not-found", "team", name);
        }
        return team;
    }

    private Team usage(CommandSender sender, String usage) {
        messages.send(sender, "error.usage", "usage", usage);
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                                @NotNull String[] args) {
        if (!sender.hasPermission("donutteams.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return TeamCommand.prefix(SUBS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        List<String> teamNames = teams.cache().teams().stream().map(Team::name).toList();
        List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 2) {
            return switch (sub) {
                case "info", "disband", "setowner", "rename", "bank", "level", "chest" -> TeamCommand.prefix(teamNames, args[1]);
                case "join", "kick" -> TeamCommand.prefix(players, args[1]);
                case "board" -> TeamCommand.prefix(List.of("create", "delete", "list"), args[1]);
                case "purge" -> TeamCommand.prefix(List.of("30", "60", "90"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            return switch (sub) {
                case "join" -> TeamCommand.prefix(teamNames, args[2]);
                case "setowner" -> {
                    Team team = teams.teamByName(args[1]);
                    yield team == null ? List.of() : TeamCommand.prefix(team.roster().stream().map(m -> m.name()).toList(), args[2]);
                }
                case "bank" -> TeamCommand.prefix(List.of("set", "give", "take"), args[2]);
                case "board" -> args[1].equalsIgnoreCase("delete") ? TeamCommand.prefix(plugin.boards().names(), args[2]) : List.of();
                default -> List.of();
            };
        }
        if (args.length == 4 && sub.equals("board") && args[1].equalsIgnoreCase("create")) {
            return TeamCommand.prefix(Arrays.stream(TopService.Stat.values()).map(TopService.Stat::key).toList(), args[3]);
        }
        return List.of();
    }
}
