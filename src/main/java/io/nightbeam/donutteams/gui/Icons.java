package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamColor;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.util.ItemCodec;
import io.nightbeam.donutteams.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Team icons (the item a team chose, or a banner in the team colour) and permission icons. */
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

    /** One recognisable item per permission, so the member menu's toggles can be told apart at a glance. */
    public static Material permission(TeamPermission permission) {
        return switch (permission) {
            case INVITE -> Material.WRITABLE_BOOK;
            case KICK -> Material.IRON_BOOTS;
            case HOME -> Material.RED_BED;
            case SETHOME -> Material.COMPASS;
            case SPEAK -> Material.OAK_SIGN;
            case PVP -> Material.IRON_SWORD;
            case BANK_DEPOSIT -> Material.GOLD_INGOT;
            case BANK_WITHDRAW -> Material.GOLD_NUGGET;
            case CHEST -> Material.ENDER_CHEST;
            case RELATIONS -> Material.WHITE_BANNER;
            case SETTINGS -> Material.COMPARATOR;
            case LEVEL_UP -> Material.EXPERIENCE_BOTTLE;
        };
    }

    public static Material banner(TeamColor color) {
        String dye = color.dye().name();
        String base = dye.endsWith("_DYE") ? dye.substring(0, dye.length() - 4) : "BLUE";
        return ItemBuilder.material(base + "_BANNER", Material.WHITE_BANNER);
    }
}
