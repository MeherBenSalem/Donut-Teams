package io.nightbeam.donutteams.command;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamColor;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.service.Format;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.service.TopService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** /team and every subcommand. With no arguments it opens the menu. */
public final class TeamCommand implements TabExecutor {

    private static final List<String> SUBS = List.of(
            "help", "create", "disband", "invite", "uninvite", "join", "decline", "request", "accept", "deny", "leave", "kick",
            "promote", "demote", "transfer", "info", "list", "top", "members", "invites", "requests", "chat", "allychat", "home",
            "sethome", "delhome", "homes", "pvp", "allypvp", "open", "rename", "tag", "description", "motd", "color", "icon",
            "bank", "deposit", "withdraw", "balance", "chest", "level", "ally", "enemy", "neutral", "relations", "settings",
            "cancel");

    private final DonutTeamsPlugin plugin;
    private final Messages messages;
    private final TeamService teams;

    public TeamCommand(DonutTeamsPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.teams = plugin.teams();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("info")) {
                Team team = teams.teamByName(args[1]);
                if (team == null) {
                    messages.send(sender, "error.team-not-found", "team", args[1]);
                } else {
                    info(sender, team, null);
                }
                return true;
            }
            messages.send(sender, "error.players-only");
            return true;
        }
        if (args.length == 0) {
            plugin.guis().openMain(player);
            return true;
        }
        String sub = alias(args[0].toLowerCase(Locale.ROOT));
        String arg = args.length >= 2 ? args[1] : null;
        String rest = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        switch (sub) {
            case "help" -> help(player);
            case "create" -> {
                if (!player.hasPermission("donutteams.create")) {
                    messages.send(player, "create.no-permission");
                } else if (arg == null) {
                    plugin.input().createTeam(player);
                } else {
                    teams.create(player, arg, args.length >= 3 ? args[2] : null);
                }
            }
            case "disband" -> teams.disband(player, arg != null && arg.equalsIgnoreCase("confirm"));
            case "invite" -> {
                if (require(player, arg, "/team invite <player>")) {
                    Player target = Bukkit.getPlayerExact(arg);
                    if (target == null || !player.canSee(target)) {
                        messages.send(player, "error.player-not-found", "player", arg);
                    } else {
                        teams.invite(player, target);
                    }
                }
            }
            case "uninvite" -> {
                if (require(player, arg, "/team uninvite <player>")) {
                    var offline = Bukkit.getOfflinePlayerIfCached(arg);
                    if (offline == null) {
                        messages.send(player, "error.player-not-found", "player", arg);
                    } else {
                        teams.revokeInvite(player, offline.getUniqueId());
                    }
                }
            }
            case "join" -> {
                if (arg == null) {
                    plugin.guis().openInvites(player);
                } else {
                    teams.join(player, arg);
                }
            }
            case "decline" -> {
                if (require(player, arg, "/team decline <team>")) {
                    teams.decline(player, arg);
                }
            }
            case "request" -> {
                if (require(player, arg, "/team request <team>")) {
                    teams.request(player, arg);
                }
            }
            case "accept" -> {
                if (require(player, arg, "/team accept <player>")) {
                    teams.acceptRequest(player, arg);
                }
            }
            case "deny" -> {
                if (require(player, arg, "/team deny <player>")) {
                    teams.denyRequest(player, arg);
                }
            }
            case "leave" -> teams.leave(player);
            case "kick" -> {
                if (require(player, arg, "/team kick <player>")) {
                    teams.kick(player, arg);
                }
            }
            case "promote" -> {
                if (require(player, arg, "/team promote <player>")) {
                    teams.promote(player, arg);
                }
            }
            case "demote" -> {
                if (require(player, arg, "/team demote <player>")) {
                    teams.demote(player, arg);
                }
            }
            case "transfer" -> {
                if (require(player, arg, "/team transfer <player>")) {
                    teams.transfer(player, arg, args.length >= 3 && args[2].equalsIgnoreCase("confirm"));
                }
            }
            case "info" -> {
                Team team = arg == null ? teams.requireTeam(player) : teams.teamByName(arg);
                if (team == null) {
                    if (arg != null) {
                        messages.send(player, "error.team-not-found", "team", arg);
                    }
                } else {
                    info(player, team, player);
                }
            }
            case "list" -> plugin.guis().openBrowse(player);
            case "top" -> plugin.guis().openTop(player, TopService.Stat.parse(arg, TopService.Stat.KILLS));
            case "members" -> plugin.guis().openMembers(player);
            case "invites" -> plugin.guis().openInvites(player);
            case "requests" -> plugin.guis().openRequests(player);
            case "chat" -> plugin.chat().teamChat(player, rest);
            case "allychat" -> plugin.chat().allyChat(player, rest);
            case "home" -> plugin.homes().start(player, arg);
            case "sethome" -> plugin.homes().setHome(player, arg);
            case "delhome" -> plugin.homes().deleteHome(player, arg);
            case "homes" -> plugin.guis().openHomes(player);
            case "pvp" -> teams.togglePvp(player);
            case "allypvp" -> teams.toggleAllyPvp(player);
            case "open" -> teams.toggleOpen(player);
            case "rename" -> {
                if (require(player, arg, "/team rename <name>")) {
                    teams.rename(player, arg);
                }
            }
            case "tag" -> {
                if (require(player, arg, "/team tag <tag>")) {
                    teams.retag(player, arg);
                }
            }
            case "description" -> {
                if (require(player, rest, "/team description <text>")) {
                    teams.setDescription(player, rest);
                }
            }
            case "motd" -> {
                if (require(player, rest, "/team motd <text>")) {
                    teams.setMotd(player, rest);
                }
            }
            case "color" -> {
                if (arg == null) {
                    plugin.guis().openColors(player);
                } else {
                    TeamColor color = TeamColor.parse(arg, null);
                    if (color == null) {
                        messages.send(player, "error.usage", "usage", "/team color <" + Arrays.stream(TeamColor.values())
                                .map(c -> c.tag()).collect(Collectors.joining("|")) + ">");
                    } else {
                        teams.setColor(player, color);
                    }
                }
            }
            case "icon" -> teams.setIcon(player);
            case "bank" -> bank(player, args);
            case "deposit" -> {
                if (require(player, arg, "/team deposit <amount>")) {
                    plugin.bank().deposit(player, arg);
                }
            }
            case "withdraw" -> {
                if (require(player, arg, "/team withdraw <amount>")) {
                    plugin.bank().withdraw(player, arg);
                }
            }
            case "balance" -> plugin.bank().balance(player);
            case "chest" -> plugin.chests().open(player);
            case "level" -> {
                if (arg != null && (arg.equalsIgnoreCase("up") || arg.equalsIgnoreCase("upgrade"))) {
                    plugin.bank().levelUp(player);
                } else {
                    plugin.guis().openLevels(player);
                }
            }
            case "ally" -> {
                if (arg == null) {
                    plugin.guis().openRelations(player);
                } else {
                    plugin.relations().ally(player, arg);
                }
            }
            case "enemy" -> {
                if (require(player, arg, "/team enemy <team>")) {
                    plugin.relations().enemy(player, arg);
                }
            }
            case "neutral" -> {
                if (require(player, arg, "/team neutral <team>")) {
                    plugin.relations().neutral(player, arg);
                }
            }
            case "relations" -> plugin.guis().openRelations(player);
            case "settings" -> plugin.guis().openSettings(player);
            case "cancel" -> plugin.chat().cancelPrompt(player);
            default -> messages.send(player, "error.unknown-command");
        }
        return true;
    }

    private void bank(Player player, String[] args) {
        if (args.length < 2) {
            if (plugin.bank().enabled()) {
                plugin.guis().openBank(player);
            } else {
                plugin.bank().balance(player);
            }
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String amount = args.length >= 3 ? args[2] : null;
        switch (action) {
            case "deposit", "d", "add" -> {
                if (require(player, amount, "/team bank deposit <amount>")) {
                    plugin.bank().deposit(player, amount);
                }
            }
            case "withdraw", "w", "take" -> {
                if (require(player, amount, "/team bank withdraw <amount>")) {
                    plugin.bank().withdraw(player, amount);
                }
            }
            default -> plugin.bank().balance(player);
        }
    }

    private boolean require(Player player, String value, String usage) {
        if (value == null || value.isBlank()) {
            messages.send(player, "error.usage", "usage", usage);
            return false;
        }
        return true;
    }

    private void help(Player player) {
        messages.sendPlain(player, "help.header");
        for (Component line : messages.list("help.lines")) {
            player.sendMessage(line);
        }
        messages.sendPlain(player, "help.footer");
    }

    /** The info block, for /team info and the console. */
    public void info(CommandSender sender, Team team, Player viewer) {
        Format format = teams.format();
        List<Component> allies = new ArrayList<>();
        for (UUID id : team.allies()) {
            Team ally = teams.teamById(id);
            if (ally != null) {
                allies.add(format.teamName(ally));
            }
        }
        List<Component> enemies = new ArrayList<>();
        for (UUID id : team.enemies()) {
            Team enemy = teams.teamById(id);
            if (enemy != null) {
                enemies.add(format.teamName(enemy));
            }
        }
        List<Component> roster = new ArrayList<>();
        for (TeamMember member : team.roster()) {
            boolean online = Bukkit.getPlayer(member.playerId()) != null;
            roster.add(Component.text(member.name(), online ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        }
        JoinConfiguration comma = JoinConfiguration.separator(Component.text(", ", NamedTextColor.DARK_GRAY));
        Object[] values = format.team(team,
                "ally_list", allies.isEmpty() ? teams.none() : Component.join(comma, allies),
                "enemy_list", enemies.isEmpty() ? teams.none() : Component.join(comma, enemies),
                "roster", Component.join(comma, roster));
        messages.sendPlain(sender, "info.header", values);
        for (Component line : messages.list("info.lines", values)) {
            sender.sendMessage(line);
        }
        if (viewer != null && !team.isMember(viewer.getUniqueId())) {
            Relation relation = teams.relation(teams.teamByPlayer(viewer.getUniqueId()), team);
            messages.sendPlain(sender, "info.relation", "relation", format.relation(relation));
        }
    }

    private static String alias(String sub) {
        return switch (sub) {
            case "c", "tc" -> "chat";
            case "ac", "ac-chat", "ally-chat" -> "allychat";
            case "desc" -> "description";
            case "colour" -> "color";
            case "echest", "ec", "vault", "enderchest" -> "chest";
            case "levelup" -> "level";
            case "bal", "money" -> "balance";
            case "show" -> "info";
            case "war" -> "enemy";
            case "peace", "unally" -> "neutral";
            case "add" -> "invite";
            case "remove" -> "kick";
            case "settag" -> "tag";
            case "warp" -> "home";
            case "setwarp" -> "sethome";
            case "delwarp" -> "delhome";
            case "warps" -> "homes";
            case "leaderboard", "lb" -> "top";
            case "ff", "friendlyfire" -> "pvp";
            default -> sub;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                                @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return prefix(SUBS, args[0]);
        }
        String sub = alias(args[0].toLowerCase(Locale.ROOT));
        Team mine = teams.teamByPlayer(player.getUniqueId());
        if (args.length == 2) {
            return switch (sub) {
                case "invite" -> prefix(Bukkit.getOnlinePlayers().stream().filter(player::canSee)
                        .filter(p -> teams.teamByPlayer(p.getUniqueId()) == null).map(Player::getName).toList(), args[1]);
                case "kick", "promote", "demote", "transfer" -> mine == null ? List.of()
                        : prefix(mine.roster().stream().filter(m -> !m.playerId().equals(player.getUniqueId())).map(TeamMember::name).toList(), args[1]);
                case "join", "decline" -> prefix(teams.cache().invitesFor(player.getUniqueId()).stream()
                        .map(invite -> teams.teamById(invite.teamId())).filter(t -> t != null).map(Team::name).toList(), args[1]);
                case "accept", "deny" -> mine == null ? List.of()
                        : prefix(teams.requests(mine.id()).stream().map(r -> r.playerName()).toList(), args[1]);
                case "uninvite" -> mine == null ? List.of() : prefix(teams.cache().teamInvites(mine.id()).stream()
                        .map(invite -> Bukkit.getOfflinePlayer(invite.playerId()).getName()).filter(n -> n != null).toList(), args[1]);
                case "info", "request", "ally", "enemy", "neutral" -> prefix(teams.cache().teams().stream()
                        .filter(t -> mine == null || !t.id().equals(mine.id()) || sub.equals("info")).map(Team::name).toList(), args[1]);
                case "home", "delhome" -> mine == null ? List.of() : prefix(mine.homeList().stream().map(h -> h.name()).toList(), args[1]);
                case "disband" -> prefix(List.of("confirm"), args[1]);
                case "top" -> prefix(Arrays.stream(TopService.Stat.values()).map(TopService.Stat::key).toList(), args[1]);
                case "color" -> prefix(Arrays.stream(TeamColor.values()).map(TeamColor::tag).toList(), args[1]);
                case "bank" -> prefix(List.of("deposit", "withdraw", "balance"), args[1]);
                case "level" -> prefix(List.of("up"), args[1]);
                case "deposit", "withdraw" -> prefix(List.of("100", "1k", "10k"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3 && sub.equals("transfer")) {
            return prefix(List.of("confirm"), args[2]);
        }
        if (args.length == 3 && sub.equals("bank")) {
            return prefix(List.of("100", "1k", "10k"), args[2]);
        }
        return List.of();
    }

    static List<String> prefix(List<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(value);
            }
        }
        return out;
    }
}
