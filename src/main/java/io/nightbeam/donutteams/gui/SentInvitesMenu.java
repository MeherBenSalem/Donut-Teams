package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.util.Durations;
import io.nightbeam.donutteams.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** Invites the team has sent; right-click takes one back. */
final class SentInvitesMenu extends Menu {

    SentInvitesMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "sent-invites");
    }

    @Override
    protected void build() {
        Team team = team();
        if (team == null) {
            new NoTeamMenu(plugin, viewer).open();
            return;
        }
        MenuConfig.Item spec = item("invite");
        long now = System.currentTimeMillis();
        paged(teams().cache().teamInvites(team.id()), invite -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(invite.playerId());
            return render(spec, ItemBuilder.head(invite.playerId()), "player", target.getName() == null ? "?" : target.getName(),
                    "expires", Durations.compact(invite.expiresAtMillis() - now));
        }, (TeamInvite invite, org.bukkit.event.inventory.ClickType click) -> {
            if (click.isRightClick()) {
                teams().revokeInvite(viewer, invite.playerId());
                redraw();
            }
        });
        back(() -> new MembersMenu(plugin, viewer).open());
    }
}
