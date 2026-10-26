package io.nightbeam.donutteams.model;

import java.util.Locale;
import org.bukkit.Material;

/** The colour a team picks for its tag: the 16 chat colours, each with a matching dye for menus. */
public enum TeamColor {
    WHITE("white", Material.WHITE_DYE),
    GRAY("gray", Material.LIGHT_GRAY_DYE),
    DARK_GRAY("dark_gray", Material.GRAY_DYE),
    BLACK("black", Material.BLACK_DYE),
    RED("red", Material.RED_DYE),
    DARK_RED("dark_red", Material.BROWN_DYE),
    GOLD("gold", Material.ORANGE_DYE),
    YELLOW("yellow", Material.YELLOW_DYE),
    GREEN("green", Material.LIME_DYE),
    DARK_GREEN("dark_green", Material.GREEN_DYE),
    AQUA("aqua", Material.LIGHT_BLUE_DYE),
    DARK_AQUA("dark_aqua", Material.CYAN_DYE),
    BLUE("blue", Material.BLUE_DYE),
    DARK_BLUE("dark_blue", Material.LAPIS_LAZULI),
    LIGHT_PURPLE("light_purple", Material.PINK_DYE),
    DARK_PURPLE("dark_purple", Material.PURPLE_DYE);

    private final String tag;
    private final Material dye;

    TeamColor(String tag, Material dye) {
        this.tag = tag;
        this.dye = dye;
    }

    /** The MiniMessage colour name, e.g. {@code light_purple}. */
    public String tag() {
        return tag;
    }

    public Material dye() {
        return dye;
    }

    public String displayName() {
        String[] words = tag.split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    public static TeamColor parse(String raw, TeamColor fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (value.equals("PURPLE")) {
            return DARK_PURPLE;
        }
        if (value.equals("PINK")) {
            return LIGHT_PURPLE;
        }
        if (value.equals("ORANGE")) {
            return GOLD;
        }
        if (value.equals("LIME")) {
            return GREEN;
        }
        if (value.equals("CYAN")) {
            return DARK_AQUA;
        }
        try {
            return TeamColor.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
