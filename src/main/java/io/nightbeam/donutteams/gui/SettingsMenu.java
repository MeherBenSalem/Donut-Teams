package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import org.bukkit.entity.Player;

/** Friendly fire, joining, colour, icon, tag, description, message of the day, name. */
final class SettingsMenu extends Menu {

    SettingsMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "settings");
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
        if (me == null) {
            return;
        }
        if (me.has(TeamPermission.PVP)) {
            place("pvp", () -> {
                teams().togglePvp(viewer);
                open();
            });
            if (plugin.settings().alliesEnabled) {
                place("ally-pvp", () -> {
                    teams().toggleAllyPvp(viewer);
                    open();
                });
            }
        }
        if (me.has(TeamPermission.SETTINGS)) {
            place("open", () -> {
                teams().toggleOpen(viewer);
                open();
            });
            place("color", special(item("color"), team), (type, slot) -> new ColorMenu(plugin, viewer).open());
            place("icon", special(item("icon"), team), (type, slot) -> {
                teams().setIcon(viewer);
                open();
            });
            place("tag", () -> plugin.input().text(viewer, "prompt.tag", tag -> {
                teams().retag(viewer, tag);
                new SettingsMenu(plugin, viewer).open();
            }));
            place("description", () -> plugin.input().text(viewer, "prompt.description", text -> {
                teams().setDescription(viewer, text);
                new SettingsMenu(plugin, viewer).open();
            }));
            place("motd", () -> plugin.input().text(viewer, "prompt.motd", text -> {
                teams().setMotd(viewer, text);
                new SettingsMenu(plugin, viewer).open();
            }));
        }
        if (me.isOwner()) {
            place("rename", () -> plugin.input().text(viewer, "prompt.rename", name -> {
                teams().rename(viewer, name);
                new SettingsMenu(plugin, viewer).open();
            }));
        }
        back(() -> new MainMenu(plugin, viewer).open());
    }
}
