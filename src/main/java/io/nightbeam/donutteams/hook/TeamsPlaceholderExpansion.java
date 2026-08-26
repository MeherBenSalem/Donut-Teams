package io.nightbeam.donutteams.hook;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.api.TeamSnapshot;
import io.nightbeam.donutteams.service.TeamService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TeamsPlaceholderExpansion extends PlaceholderExpansion {

    private final DonutTeamsPlugin plugin;
    private final TeamService teams;

    public TeamsPlaceholderExpansion(DonutTeamsPlugin plugin, TeamService teams) {
        this.plugin = plugin;
        this.teams = teams;
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
        if (player == null) {
            return "";
        }
        TeamSnapshot snapshot = teams.snapshot(player.getUniqueId()).orElse(null);
        String none = plugin.messages().raw("placeholders.none", "none");
        return switch (params.toLowerCase()) {
            case "name", "team", "team_name" -> snapshot == null ? none : snapshot.name();
            case "tag" -> snapshot == null ? none : snapshot.tag();
            case "count", "size", "members" -> snapshot == null ? "0" : String.valueOf(snapshot.memberCount());
            case "home_world", "homeworld", "home" -> snapshot == null || snapshot.homeWorld() == null ? none : snapshot.homeWorld();
            case "leader", "owner" -> {
                if (snapshot == null) {
                    yield none;
                }
                var team = teams.teamById(snapshot.id());
                if (team == null || team.member(snapshot.ownerId()) == null) {
                    yield none;
                }
                yield team.member(snapshot.ownerId()).name();
            }
            default -> null;
        };
    }
}
