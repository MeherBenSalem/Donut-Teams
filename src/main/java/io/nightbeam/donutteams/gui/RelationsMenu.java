package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Allies, enemies (declared by us or on us) and incoming ally requests. */
final class RelationsMenu extends Menu {

    private record Entry(Team team, String kind) {
    }

    RelationsMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "relations");
    }

    /** "enemy-of-us" is new in 2.0.2: menus.yml files from older versions don't have it, so fall back to "enemy". */
    private io.nightbeam.donutteams.gui.MenuConfig.Item itemFor(String kind) {
        var configured = item(kind);
        return configured != null || !kind.equals("enemy-of-us") ? configured : item("enemy");
    }

    @Override
    protected void build() {
        Team team = team();
        if (team == null) {
            new NoTeamMenu(plugin, viewer).open();
            return;
        }
        TeamMember me = team.member(viewer.getUniqueId());
        boolean manage = me != null && me.has(TeamPermission.RELATIONS);
        List<Entry> entries = new ArrayList<>();
        for (Team request : plugin.relations().incoming(team.id())) {
            entries.add(new Entry(request, "request"));
        }
        for (UUID id : team.allies()) {
            Team ally = teams().teamById(id);
            if (ally != null) {
                entries.add(new Entry(ally, "ally"));
            }
        }
        for (UUID id : team.enemies()) {
            Team enemy = teams().teamById(id);
            if (enemy != null) {
                entries.add(new Entry(enemy, "enemy"));
            }
        }
        for (Team declarer : plugin.relations().declaredOn(team.id())) {
            entries.add(new Entry(declarer, "enemy-of-us"));
        }
        paged(entries, entry -> render(itemFor(entry.kind()), Icons.team(entry.team()), format().team(entry.team())), (entry, click) -> {
            if (!manage) {
                return;
            }
            switch (entry.kind()) {
                case "request" -> plugin.relations().ally(viewer, entry.team().name());
                case "enemy-of-us" -> {
                    // They declared the war; only they can end it. Show who they are.
                    new ProfileMenu(plugin, viewer, entry.team().id(), () -> new RelationsMenu(plugin, viewer).open()).open();
                    return;
                }
                case "ally", "enemy" -> {
                    if (!click.isRightClick()) {
                        new ProfileMenu(plugin, viewer, entry.team().id(), () -> new RelationsMenu(plugin, viewer).open()).open();
                        return;
                    }
                    plugin.relations().neutral(viewer, entry.team().name());
                }
                default -> {
                    return;
                }
            }
            open();
        });
        if (manage && plugin.settings().alliesEnabled) {
            place("add-ally", () -> plugin.input().text(viewer, "prompt.ally", name -> {
                plugin.relations().ally(viewer, name);
                new RelationsMenu(plugin, viewer).open();
            }));
        }
        if (manage && plugin.settings().enemiesEnabled) {
            place("add-enemy", () -> plugin.input().text(viewer, "prompt.enemy", name -> {
                plugin.relations().enemy(viewer, name);
                new RelationsMenu(plugin, viewer).open();
            }));
        }
        back(() -> new MainMenu(plugin, viewer).open());
    }

    static Relation relation(DonutTeamsPlugin plugin, Team mine, Team other) {
        return plugin.teams().relation(mine, other);
    }
}
