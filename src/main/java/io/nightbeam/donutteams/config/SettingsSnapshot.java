package io.nightbeam.donutteams.config;

import java.util.Locale;
import java.util.Map;

/**
 * Immutable view of {@code config.yml} keys used by {@link PluginSettings}.
 * Parses nested YAML maps without Bukkit so unit tests can cover defaults.
 */
public final class SettingsSnapshot {

    public static final String SQLITE_FILE = "storage.sqlite.file";
    public static final String DEFAULT_MAX_MEMBERS = "teams.default-max-members";
    public static final String LITE_MAX_MEMBERS = "teams.lite-max-members";
    public static final String NAME_MIN = "teams.name-min";
    public static final String NAME_MAX = "teams.name-max";
    public static final String TAG_MIN = "teams.tag-min";
    public static final String TAG_MAX = "teams.tag-max";
    public static final String INVITE_EXPIRE_SECONDS = "teams.invite-expire-seconds";
    public static final String HOME_WARMUP_SECONDS = "home.warmup-seconds";
    public static final String HOME_CANCEL_ON_MOVE = "home.cancel-on-move";
    public static final String CHAT_ENABLED = "chat.enabled";
    public static final String DEFAULT_FRIENDLY_FIRE = "pvp.default-friendly-fire";
    public static final String METRICS_ENABLED = "metrics.enabled";
    public static final String BSTATS_ID = "metrics.bstats-id";

    private final String sqliteFile;
    private final int defaultMaxMembers;
    private final int liteMaxMembers;
    private final int nameMin;
    private final int nameMax;
    private final int tagMin;
    private final int tagMax;
    private final int inviteExpireSeconds;
    private final int homeWarmupSeconds;
    private final boolean cancelHomeOnMove;
    private final boolean chatEnabled;
    private final boolean defaultFriendlyFire;
    private final boolean metricsEnabled;
    private final int bstatsId;

    public SettingsSnapshot(
            String sqliteFile,
            int defaultMaxMembers,
            int liteMaxMembers,
            int nameMin,
            int nameMax,
            int tagMin,
            int tagMax,
            int inviteExpireSeconds,
            int homeWarmupSeconds,
            boolean cancelHomeOnMove,
            boolean chatEnabled,
            boolean defaultFriendlyFire,
            boolean metricsEnabled,
            int bstatsId
    ) {
        this.sqliteFile = sqliteFile == null || sqliteFile.isBlank() ? "teams.db" : sqliteFile;
        this.defaultMaxMembers = defaultMaxMembers;
        this.liteMaxMembers = liteMaxMembers;
        this.nameMin = nameMin;
        this.nameMax = nameMax;
        this.tagMin = tagMin;
        this.tagMax = tagMax;
        this.inviteExpireSeconds = inviteExpireSeconds;
        this.homeWarmupSeconds = homeWarmupSeconds;
        this.cancelHomeOnMove = cancelHomeOnMove;
        this.chatEnabled = chatEnabled;
        this.defaultFriendlyFire = defaultFriendlyFire;
        this.metricsEnabled = metricsEnabled;
        this.bstatsId = bstatsId;
    }

    public static SettingsSnapshot defaults() {
        return fromMap(Map.of());
    }

    @SuppressWarnings("unchecked")
    public static SettingsSnapshot fromMap(Map<String, Object> root) {
        Map<String, Object> yaml = root == null ? Map.of() : root;
        return new SettingsSnapshot(
                stringAt(yaml, SQLITE_FILE, "teams.db"),
                intAt(yaml, DEFAULT_MAX_MEMBERS, 8),
                intAt(yaml, LITE_MAX_MEMBERS, 8),
                intAt(yaml, NAME_MIN, 3),
                intAt(yaml, NAME_MAX, 16),
                intAt(yaml, TAG_MIN, 2),
                intAt(yaml, TAG_MAX, 6),
                intAt(yaml, INVITE_EXPIRE_SECONDS, 300),
                intAt(yaml, HOME_WARMUP_SECONDS, 3),
                boolAt(yaml, HOME_CANCEL_ON_MOVE, true),
                boolAt(yaml, CHAT_ENABLED, true),
                boolAt(yaml, DEFAULT_FRIENDLY_FIRE, false),
                boolAt(yaml, METRICS_ENABLED, true),
                intAt(yaml, BSTATS_ID, 0)
        );
    }

    public String sqliteFile() {
        return sqliteFile;
    }

    public int defaultMaxMembers() {
        return defaultMaxMembers;
    }

    public int liteMaxMembers() {
        return liteMaxMembers;
    }

    public int nameMin() {
        return nameMin;
    }

    public int nameMax() {
        return nameMax;
    }

    public int tagMin() {
        return tagMin;
    }

    public int tagMax() {
        return tagMax;
    }

    public int inviteExpireSeconds() {
        return inviteExpireSeconds;
    }

    public int homeWarmupSeconds() {
        return homeWarmupSeconds;
    }

    public boolean cancelHomeOnMove() {
        return cancelHomeOnMove;
    }

    public boolean chatEnabled() {
        return chatEnabled;
    }

    public boolean defaultFriendlyFire() {
        return defaultFriendlyFire;
    }

    public boolean metricsEnabled() {
        return metricsEnabled;
    }

    public int bstatsId() {
        return bstatsId;
    }

    static String stringAt(Map<String, Object> root, String path, String fallback) {
        Object value = valueAt(root, path);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    static int intAt(Map<String, Object> root, String path, int fallback) {
        Object value = valueAt(root, path);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    static boolean boolAt(Map<String, Object> root, String path, boolean fallback) {
        Object value = valueAt(root, path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
                return false;
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    static Object valueAt(Map<String, Object> root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}
