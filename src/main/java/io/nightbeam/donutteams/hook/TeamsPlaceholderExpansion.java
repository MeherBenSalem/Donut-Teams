package io.nightbeam.donutteams.hook;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.service.Format;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.service.TopService;
import io.nightbeam.donutteams.util.Amounts;
import java.util.List;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * %donutteams_...% placeholders, plus relational ones (%rel_donutteams_relation_color%) so tab and name tag plugins
 * can colour teammates green, allies aqua and enemies red. Every lookup reads memory only.
 */
public final class TeamsPlaceholderExpansion extends PlaceholderExpansion implements Relational {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final DonutTeamsPlugin plugin;
    private final TeamService teams;

    public TeamsPlaceholderExpansion(DonutTeamsPlugin plugin) {
        this.plugin = plugin;
        this.teams = plugin.teams();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "donutteams";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nightbeam Studio";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.startsWith("top_")) {
            return top(key);
        }
        Team team = player == null ? null : teams.teamByPlayer(player.getUniqueId());
        Format format = teams.format();
        String none = plugin.messages().raw("placeholders.none");
        if (key.equals("has_team")) {
            return String.valueOf(team != null);
        }
        if (team == null) {
            return switch (key) {
                case "name", "team", "team_name", "name_formatted" -> plugin.messages().raw("placeholders.no-team");
                case "count", "size", "members", "members_online", "kills", "deaths", "level", "balance", "allies", "enemies",
                     "homes" -> "0";
                default -> none;
            };
        }
        TeamMember member = team.member(player.getUniqueId());
        return switch (key) {
            case "name", "team", "team_name" -> team.name();
            case "name_formatted" -> legacy(format.teamName(team));
            case "tag" -> team.tag();
            case "tag_formatted" -> legacy(format.tag(team));
            case "color" -> team.color().tag();
            case "color_code" -> legacy(Component.text("x", format.textColor(team))).replace("x", "");
            case "role" -> member == null ? none : plugin.messages().plain(format.roleComponent(member.role()));
            case "role_formatted" -> member == null ? none : legacy(format.roleComponent(member.role()));
            case "leader", "owner" -> team.owner() == null ? none : team.owner().name();
            case "count", "size", "members" -> String.valueOf(team.size());
            case "members_online", "online" -> String.valueOf(format.online(team));
            case "max_members" -> String.valueOf(format.maxMembers(team));
            case "level" -> String.valueOf(team.level());
            case "balance" -> String.format(Locale.ROOT, "%.2f", team.balance());
            case "balance_formatted" -> format.money(team.balance());
            case "balance_short" -> Amounts.shortForm(team.balance());
            case "kills" -> String.valueOf(team.kills());
            case "deaths" -> String.valueOf(team.deaths());
            case "kdr" -> format.kdr(team);
            case "player_kills" -> member == null ? "0" : String.valueOf(member.kills());
            case "player_deaths" -> member == null ? "0" : String.valueOf(member.deaths());
            case "allies" -> String.valueOf(team.allies().size());
            case "enemies" -> String.valueOf(team.enemies().size());
            case "homes" -> String.valueOf(team.homes().size());
            case "max_homes" -> String.valueOf(format.maxHomes(team));
            case "home_world", "homeworld", "home" -> team.defaultHome() == null ? none : team.defaultHome().world();
            case "open" -> String.valueOf(team.open());
            case "pvp", "friendly_fire" -> String.valueOf(team.friendlyFire());
            case "description" -> team.description();
            case "motd" -> team.motd();
            case "chat_mode" -> plugin.chat().allyToggled(player.getUniqueId()) ? "ally"
                    : plugin.chat().teamToggled(player.getUniqueId()) ? "team" : "public";
            case "rank_kills" -> String.valueOf(plugin.top().rank(team.id(), TopService.Stat.KILLS));
            case "rank_balance" -> String.valueOf(plugin.top().rank(team.id(), TopService.Stat.BALANCE));
            case "rank_level" -> String.valueOf(plugin.top().rank(team.id(), TopService.Stat.LEVEL));
            case "rank_members" -> String.valueOf(plugin.top().rank(team.id(), TopService.Stat.MEMBERS));
            default -> null;
        };
    }

    /** %donutteams_top_kills_1_name%, _tag, _value. */
    private String top(String key) {
        String[] parts = key.split("_");
        if (parts.length != 4) {
            return null;
        }
        TopService.Stat stat = TopService.Stat.parse(parts[1], null);
        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (stat == null || position < 1) {
            return null;
        }
        List<Team> top = plugin.top().top(stat);
        if (position > top.size()) {
            return parts[3].equals("value") ? "0" : "-";
        }
        Team team = top.get(position - 1);
        return switch (parts[3]) {
            case "name" -> team.name();
            case "name_formatted" -> legacy(teams.format().teamName(team));
            case "tag" -> team.tag();
            case "value" -> switch (stat) {
                case KILLS -> String.valueOf(team.kills());
                case BALANCE -> Amounts.shortForm(team.balance());
                case LEVEL -> String.valueOf(team.level());
                case MEMBERS -> String.valueOf(team.size());
            };
            default -> null;
        };
    }

    @Override
    public String onPlaceholderRequest(Player one, Player two, String identifier) {
        if (one == null || two == null) {
            return "";
        }
        Relation relation = teams.relation(one.getUniqueId(), two.getUniqueId());
        Format format = teams.format();
        Team theirs = teams.teamByPlayer(two.getUniqueId());
        return switch (identifier.toLowerCase(Locale.ROOT)) {
            case "relation" -> plugin.messages().plain(format.relation(relation));
            case "relation_formatted" -> legacy(format.relation(relation));
            case "relation_color" -> legacy(Component.text("x", format.relationColor(relation))).replace("x", "");
            case "tag" -> theirs == null ? "" : legacy(Component.text(theirs.tag(), format.relationColor(relation)));
            case "tag_bracketed" -> theirs == null ? "" : legacy(Component.text("[" + theirs.tag() + "]", format.relationColor(relation)));
            case "name" -> theirs == null ? "" : legacy(Component.text(theirs.name(), format.relationColor(relation)));
            default -> null;
        };
    }

    private static String legacy(Component component) {
        return LEGACY.serialize(component);
    }
}
