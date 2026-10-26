package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.TeamColor;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** The sixteen team colours. */
final class ColorMenu extends Menu {

    ColorMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "color");
    }

    @Override
    protected void build() {
        MenuConfig.Item spec = item("color");
        paged(List.of(TeamColor.values()), color -> {
            NamedTextColor text = NamedTextColor.NAMES.value(color.tag());
            Component display = Component.text(color.displayName(), text == null ? NamedTextColor.WHITE : text);
            return render(spec, new ItemStack(color.dye()), "color_display", display);
        }, (color, click) -> {
            teams().setColor(viewer, color);
            new SettingsMenu(plugin, viewer).open();
        });
        back(() -> new SettingsMenu(plugin, viewer).open());
    }
}
