package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.service.Format;
import io.nightbeam.donutteams.util.Durations;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** Invites the viewer has received: left-click joins, right-click declines. */
final class InvitesMenu extends Menu {

    InvitesMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "invites");
    }

    @Override
    protected void build() {
        List<TeamInvite> invites = new ArrayList<>(teams().cache().invitesFor(viewer.getUniqueId()));
        MenuConfig.Item spec = item("invite");
        long now = System.currentTimeMillis();
        paged(invites, invite -> {
            Team team = teams().teamById(invite.teamId());
            OfflinePlayer inviter = Bukkit.getOfflinePlayer(invite.invitedBy());
            Object[] values = Format.concat(format().team(team), "inviter", inviter.getName() == null ? "?" : inviter.getName(),
                    "expires", Durations.compact(invite.expiresAtMillis() - now));
            return render(spec, Icons.team(team), values);
        }, (invite, click) -> {
            Team team = teams().teamById(invite.teamId());
            if (team == null) {
                redraw();
                return;
            }
            if (click.isRightClick()) {
                teams().decline(viewer, team.name());
                redraw();
            } else {
                close();
                teams().join(viewer, team.name());
            }
        });
        back(() -> plugin.guis().openMain(viewer));
    }
}
