package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.service.TopService;
import org.bukkit.entity.Player;

/** /team for a player without a team: create, invites, browse, top. */
final class NoTeamMenu extends Menu {

    NoTeamMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "no-team");
    }

    @Override
    protected Object[] placeholders() {
        return new Object[] {"count", teams().cache().invitesFor(viewer.getUniqueId()).size(), "teams", teams().cache().size()};
    }

    @Override
    protected void build() {
        place("create", () -> plugin.input().createTeam(viewer));
        place("invites", () -> new InvitesMenu(plugin, viewer).open());
        place("browse", () -> new BrowseMenu(plugin, viewer).open());
        place("top", () -> new TopMenu(plugin, viewer, TopService.Stat.KILLS).open());
    }
}
