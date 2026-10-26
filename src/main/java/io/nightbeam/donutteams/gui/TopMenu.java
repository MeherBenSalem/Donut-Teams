package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.service.Format;
import io.nightbeam.donutteams.service.TopService;
import io.nightbeam.donutteams.util.Amounts;
import java.util.List;
import org.bukkit.entity.Player;

/** Leaderboards by kills, bank, level or members. */
final class TopMenu extends Menu {

    private TopService.Stat stat;

    TopMenu(DonutTeamsPlugin plugin, Player viewer, TopService.Stat stat) {
        super(plugin, viewer, "top");
        this.stat = stat;
    }

    @Override
    protected Object[] placeholders() {
        return new Object[] {"stat_name", messages().raw("stats." + stat.key())};
    }

    @Override
    protected void build() {
        List<Team> top = plugin.top().top(stat);
        MenuConfig.Item spec = item("team");
        paged(top, team -> render(spec, Icons.team(team), Format.concat(format().team(team),
                "rank", String.valueOf(top.indexOf(team) + 1), "value", value(plugin, team, stat))),
                (team, click) -> new ProfileMenu(plugin, viewer, team.id(), () -> new TopMenu(plugin, viewer, stat).open()).open());
        for (TopService.Stat tab : TopService.Stat.values()) {
            place("tab-" + tab.key(), null, (type, slot) -> {
                stat = tab;
                page = 0;
                open();
            });
        }
        back(() -> plugin.guis().openMain(viewer));
    }

    static String value(DonutTeamsPlugin plugin, Team team, TopService.Stat stat) {
        return switch (stat) {
            case KILLS -> String.valueOf(team.kills());
            case BALANCE -> plugin.teams().format().money(team.balance());
            case LEVEL -> String.valueOf(team.level());
            case MEMBERS -> String.valueOf(team.size());
        };
    }

    static String shortValue(Team team, TopService.Stat stat) {
        return stat == TopService.Stat.BALANCE ? "$" + Amounts.shortForm(team.balance()) : null;
    }
}
