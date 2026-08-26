package io.nightbeam.donutteams.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SlotPermissionParser {

    public static final String PREFIX = "donutteams.slots.";
    private static final Pattern NODE = Pattern.compile("^donutteams\\.slots\\.(\\d+)$", Pattern.CASE_INSENSITIVE);

    private SlotPermissionParser() {
    }

    public static int parseHighest(Set<String> permissions, int defaultSlots, int liteCap) {
        int highest = Math.max(1, defaultSlots);
        if (permissions != null) {
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
        }
        return Math.min(highest, Math.max(1, liteCap));
    }

    public static boolean isSlotNode(String permission) {
        return permission != null && NODE.matcher(permission.trim()).matches();
    }
}
