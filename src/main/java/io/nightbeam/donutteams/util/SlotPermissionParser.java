package io.nightbeam.donutteams.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads {@code donutteams.slots.<n>} permission nodes. */
public final class SlotPermissionParser {

    public static final String PREFIX = "donutteams.slots.";
    private static final Pattern NODE = Pattern.compile("^donutteams\\.slots\\.(\\d+)$", Pattern.CASE_INSENSITIVE);

    private SlotPermissionParser() {
    }

    /** The highest slot node in the set, or 0 when there is none. */
    public static int highest(Set<String> permissions) {
        int highest = 0;
        if (permissions == null) {
            return 0;
        }
        for (String permission : permissions) {
            if (permission == null) {
                continue;
            }
            Matcher matcher = NODE.matcher(permission.trim().toLowerCase(Locale.ROOT));
            if (!matcher.matches()) {
                continue;
            }
            try {
                highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                // Ignore malformed numeric suffixes.
            }
        }
        return highest;
    }

    /** The member cap: the level's cap, raised by a higher slot node, never above the hard cap. */
    public static int cap(int levelCap, int slotNode, int hardCap) {
        int cap = Math.max(1, Math.max(levelCap, slotNode));
        return hardCap > 0 ? Math.min(cap, hardCap) : cap;
    }

    public static boolean isSlotNode(String permission) {
        return permission != null && NODE.matcher(permission.trim()).matches();
    }
}
