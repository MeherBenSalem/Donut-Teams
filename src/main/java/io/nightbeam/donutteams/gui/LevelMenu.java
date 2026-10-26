package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.config.LevelTable;
import io.nightbeam.donutteams.model.Team;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Every level with its perks; the next one can be bought from the bank. */
final class LevelMenu extends Menu {

    LevelMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "level");
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
        List<LevelTable.Level> levels = new ArrayList<>(plugin.settings().levels.all());
        MenuConfig.Item base = item("level");
        paged(levels, level -> {
            String state = level.level() <= team.level() ? "reached" : level.level() == team.level() + 1 ? "next" : "locked";
            MenuConfig.Item look = item(state);
            Material material = look == null ? Material.BOOK : material(look.material());
            MenuConfig.Item spec = new MenuConfig.Item(List.of(), look == null ? "BOOK" : look.material(),
                    look == null ? "Level <number>" : look.name(), base == null ? List.of() : base.lore(), look == null ? null : look.raw());
            return render(spec, new ItemStack(material),
                    "number", level.level(),
                    "level_members", level.maxMembers(),
                    "level_homes", level.maxHomes(),
                    "level_rows", level.chestRows(),
                    "level_cost", level.cost() <= 0 ? messages().component("format.none") : format().money(level.cost()),
                    "state", messages().render(look == null ? "" : look.string("state", ""), placeholders()));
        }, (level, click) -> {
            if (level.level() == team.level() + 1) {
                plugin.bank().levelUp(viewer);
                plugin.scheduler().runLaterForEntity(viewer, () -> new LevelMenu(plugin, viewer).open(), 2L);
            }
        });
        back(() -> new MainMenu(plugin, viewer).open());
    }
}
