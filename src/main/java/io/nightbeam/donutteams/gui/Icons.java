package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamColor;
import io.nightbeam.donutteams.util.ItemCodec;
import io.nightbeam.donutteams.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Team icons: the item a team chose, or a banner in the team colour. */
public final class Icons {

    private Icons() {
    }

    public static ItemStack team(Team team) {
        ItemStack custom = ItemCodec.decode(team.iconData());
        if (custom != null && custom.getType() != Material.AIR) {
            custom.setAmount(1);
            return custom;
        }
        return new ItemStack(banner(team.color()));
    }

    public static Material banner(TeamColor color) {
        String dye = color.dye().name();
        String base = dye.endsWith("_DYE") ? dye.substring(0, dye.length() - 4) : "BLUE";
        return ItemBuilder.material(base + "_BANNER", Material.WHITE_BANNER);
    }
}
