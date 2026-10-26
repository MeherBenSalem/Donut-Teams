package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.entity.Player;

/** Every team, busiest first. */
final class BrowseMenu extends Menu {

    BrowseMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "browse");
    }

    @Override
    protected Object[] placeholders() {
        return new Object[] {"teams", teams().cache().size()};
    }

    @Override
    protected void build() {
        List<Team> all = new ArrayList<>(teams().cache().teams());
        all.sort(Comparator.comparingInt((Team team) -> -format().online(team))
                .thenComparingInt(team -> -team.size())
                .thenComparing(team -> team.name().toLowerCase()));
        MenuConfig.Item spec = item("team");
        paged(all, team -> render(spec, Icons.team(team), format().team(team)),
                (team, click) -> new ProfileMenu(plugin, viewer, team.id(), () -> new BrowseMenu(plugin, viewer).open()).open());
        back(() -> plugin.guis().openMain(viewer));
    }
}
