package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.util.Durations;
import io.nightbeam.donutteams.util.ItemBuilder;
import org.bukkit.entity.Player;

/** Join requests: left-click accepts, right-click denies. */
final class RequestsMenu extends Menu {

    RequestsMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "requests");
    }

    @Override
    protected void build() {
        Team team = team();
        if (team == null) {
            new NoTeamMenu(plugin, viewer).open();
            return;
        }
        MenuConfig.Item spec = item("request");
        long now = System.currentTimeMillis();
        paged(teams().requests(team.id()), request -> render(spec, ItemBuilder.head(request.playerId()),
                "player", request.playerName(), "expires", Durations.compact(request.expiresAtMillis() - now)), (request, click) -> {
                    if (click.isRightClick()) {
                        teams().denyRequest(viewer, request.playerName());
                    } else {
                        teams().acceptRequest(viewer, request.playerName());
                    }
                    plugin.scheduler().runLaterForEntity(viewer, this::redraw, 2L);
                });
        back(() -> new MembersMenu(plugin, viewer).open());
    }
}
