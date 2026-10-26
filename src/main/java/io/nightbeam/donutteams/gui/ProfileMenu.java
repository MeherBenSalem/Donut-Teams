package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Another team: details, roster, and join or diplomacy buttons. */
final class ProfileMenu extends Menu {

    private final UUID teamId;
    private final Runnable backAction;

    ProfileMenu(DonutTeamsPlugin plugin, Player viewer, UUID teamId, Runnable backAction) {
        super(plugin, viewer, "profile");
        this.teamId = teamId;
        this.backAction = backAction;
    }

    @Override
    protected Object[] placeholders() {
        Team team = teams().teamById(teamId);
        if (team == null) {
            return new Object[0];
        }
        Relation relation = teams().relation(team(), team);
        return format().team(team, "relation", format().relation(relation));
    }

    @Override
    protected void build() {
        Team team = teams().teamById(teamId);
        if (team == null) {
            backAction.run();
            return;
        }
        place("info", Icons.team(team), null);
        MenuConfig.Item spec = item("member");
        paged(team.roster(), member -> render(spec, ItemBuilder.head(member.playerId()),
                MembersMenu.memberValues(plugin, member, "")), null);
        Team mine = team();
        if (mine == null) {
            place("join", () -> {
                close();
                if (team.open() || teams().cache().invite(team.id(), viewer.getUniqueId()) != null) {
                    teams().join(viewer, team.name());
                } else {
                    teams().request(viewer, team.name());
                }
            });
        } else if (!mine.id().equals(team.id())) {
            TeamMember me = mine.member(viewer.getUniqueId());
            if (me != null && me.has(TeamPermission.RELATIONS)) {
                if (plugin.settings().alliesEnabled && !mine.alliedWith(team.id())) {
                    place("ally", () -> {
                        plugin.relations().ally(viewer, team.name());
                        open();
                    });
                }
                if (plugin.settings().enemiesEnabled && !mine.enemyOf(team.id())) {
                    place("enemy", () -> {
                        plugin.relations().enemy(viewer, team.name());
                        open();
                    });
                }
                if (mine.alliedWith(team.id()) || mine.enemyOf(team.id())) {
                    place("neutral", () -> {
                        plugin.relations().neutral(viewer, team.name());
                        open();
                    });
                }
            }
        }
        back(backAction);
    }
}
