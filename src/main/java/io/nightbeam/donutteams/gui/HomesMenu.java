package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Team homes: click to teleport, shift + right-click to delete, and a button to set one here. */
final class HomesMenu extends Menu {

    HomesMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "homes");
    }

    @Override
    protected Object[] placeholders() {
        Team team = team();
        return team == null ? new Object[0] : format().team(team);
    }

    @Override
    protected void build() {
        Team team = team();
        if (team == null) {
            new NoTeamMenu(plugin, viewer).open();
            return;
        }
        TeamMember me = team.member(viewer.getUniqueId());
        boolean manage = me != null && me.has(TeamPermission.SETHOME);
        MenuConfig.Item spec = item("home");
        paged(team.homeList(), home -> render(spec, null, values(home)), (home, click) -> {
            if (click == ClickType.SHIFT_RIGHT && manage) {
                plugin.homes().deleteHome(viewer, home.name());
                open();
                return;
            }
            close();
            plugin.homes().start(viewer, home.name());
        });
        if (manage) {
            place("add", () -> {
                if (team.homes().isEmpty()) {
                    close();
                    plugin.homes().setHome(viewer, TeamHome.DEFAULT);
                    return;
                }
                plugin.input().text(viewer, "prompt.home-name", name -> {
                    plugin.homes().setHome(viewer, name);
                    new HomesMenu(plugin, viewer).open();
                });
            });
        }
        back(() -> new MainMenu(plugin, viewer).open());
    }

    private Object[] values(TeamHome home) {
        OfflinePlayer setter = home.setBy() == null ? null : Bukkit.getOfflinePlayer(home.setBy());
        return new Object[] {"home", home.name(), "world", home.world(), "coords", home.coordinates(),
                "setter", setter == null || setter.getName() == null ? "?" : setter.getName()};
    }
}
