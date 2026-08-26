package io.nightbeam.donutteams.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TeamNameValidator {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern TAG = Pattern.compile("^[A-Za-z0-9]{2,6}$");

    private TeamNameValidator() {
    }

    public static boolean validName(String name, int min, int max) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            return false;
        }
        return NAME.matcher(trimmed).matches();
    }

    public static boolean validTag(String tag, int min, int max) {
        if (tag == null) {
            return false;
        }
        String trimmed = tag.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            return false;
        }
        return TAG.matcher(trimmed).matches();
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    public static String normalizeTag(String tag) {
        return tag == null ? "" : tag.trim();
    }

    public static String nameKey(String name) {
        return normalizeName(name).toLowerCase(Locale.ROOT);
    }
}
