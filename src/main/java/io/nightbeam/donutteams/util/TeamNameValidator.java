package io.nightbeam.donutteams.util;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TeamNameValidator {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern TAG = Pattern.compile("^[A-Za-z0-9]+$");

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

    /** Whether a name or tag contains a blocked word (case-insensitive, ignoring underscores). */
    public static boolean blocked(String value, Collection<String> blockedWords) {
        if (value == null || blockedWords == null || blockedWords.isEmpty()) {
            return false;
        }
        String flat = value.toLowerCase(Locale.ROOT).replace("_", "");
        for (String word : blockedWords) {
            if (word == null || word.isBlank()) {
                continue;
            }
            if (flat.contains(word.toLowerCase(Locale.ROOT).replace("_", "").trim())) {
                return true;
            }
        }
        return false;
    }

    /** A tag derived from a team name: its letters and digits, cut to {@code max}. */
    public static String tagFromName(String name, int max) {
        String letters = name == null ? "" : name.replaceAll("[^A-Za-z0-9]", "");
        int limit = Math.max(1, max);
        return letters.length() > limit ? letters.substring(0, limit) : letters;
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
