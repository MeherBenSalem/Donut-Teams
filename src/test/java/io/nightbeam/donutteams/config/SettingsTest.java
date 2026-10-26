package io.nightbeam.donutteams.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.storage.DatabaseType;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsTest {

    private static YamlConfiguration shipped(String name) throws Exception {
        try (var reader = new InputStreamReader(SettingsTest.class.getResourceAsStream("/" + name), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    @Test
    void shippedConfigHasTheDocumentedDefaults() throws Exception {
        Settings settings = Settings.from(shipped("config.yml"));
        assertEquals(DatabaseType.SQLITE, settings.storageType);
        assertEquals("teams.db", settings.sqliteFile);
        assertEquals(3, settings.nameMin);
        assertEquals(16, settings.nameMax);
        assertEquals(2, settings.tagMin);
        assertEquals(6, settings.tagMax);
        assertEquals(300, settings.inviteExpireSeconds);
        assertEquals(3, settings.homeWarmupSeconds);
        assertTrue(settings.homeCancelOnMove);
        assertTrue(settings.chatEnabled);
        assertFalse(settings.defaultFriendlyFire);
        assertEquals(5, settings.levels.max());
        assertEquals(8, settings.levels.get(1).maxMembers());
        assertEquals(6, settings.levels.get(5).chestRows());
        assertTrue(settings.blockedNames.contains("admin"));
        assertEquals(50, settings.maxMembersCap);
    }

    @Test
    void rolesGetTheirConfiguredPermissions() throws Exception {
        Settings settings = Settings.from(shipped("config.yml"));
        assertTrue(settings.defaultPermissions(TeamRole.MEMBER).contains(TeamPermission.SPEAK));
        assertTrue(settings.defaultPermissions(TeamRole.MEMBER).contains(TeamPermission.CHEST));
        assertFalse(settings.defaultPermissions(TeamRole.MEMBER).contains(TeamPermission.KICK));
        assertTrue(settings.defaultPermissions(TeamRole.OFFICER).contains(TeamPermission.KICK));
        assertFalse(settings.defaultPermissions(TeamRole.OFFICER).contains(TeamPermission.LEVEL_UP));
        assertEquals(TeamPermission.all(), settings.defaultPermissions(TeamRole.OWNER));
    }

    @Test
    void missingKeysFallBackToDefaults() {
        Settings settings = Settings.defaults();
        assertEquals("teams.db", settings.sqliteFile);
        assertEquals(1, settings.levels.max());
        assertEquals(8, settings.levels.get(1).maxMembers());
        assertEquals(300, settings.inviteExpireSeconds);
        assertTrue(settings.chatEnabled);
        assertTrue(settings.bankEnabled);
    }

    @Test
    void mysqlIsReadAsMariaDbAndPrefixIsSanitised() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("storage.type", "mysql");
        yaml.set("storage.table-prefix", "dt_; DROP TABLE x");
        Settings settings = Settings.from(yaml);
        assertEquals(DatabaseType.MARIADB, settings.storageType);
        assertEquals("dt_DROPTABLEx", settings.tablePrefix);
    }

    @Test
    void levelTableStopsAtTheFirstGapAndNextIsNullAtTheTop() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("levels.list.1.max-members", 5);
        yaml.set("levels.list.2.max-members", 7);
        yaml.set("levels.list.2.cost", 100);
        yaml.set("levels.list.4.max-members", 99);
        Settings settings = Settings.from(yaml);
        assertEquals(2, settings.levels.max());
        assertEquals(7, settings.levels.next(1).maxMembers());
        assertNull(settings.levels.next(2));
        assertEquals(7, settings.levels.get(9).maxMembers());
    }

    @Test
    void shippedMessagesKeepOnAndOffAsTextKeys() throws Exception {
        YamlConfiguration messages = shipped("messages.yml");
        assertEquals("<green>On", messages.getString("format.on"));
        assertEquals("<red>Off", messages.getString("format.off"));
        assertTrue(messages.getInt("messages-version") >= 2);
    }

    @Test
    void shippedFilesHaveNoDuplicateKeys() throws Exception {
        org.yaml.snakeyaml.LoaderOptions options = new org.yaml.snakeyaml.LoaderOptions();
        options.setAllowDuplicateKeys(false);
        for (String name : new String[] {"config.yml", "messages.yml", "menus.yml", "plugin.yml"}) {
            try (var stream = SettingsTest.class.getResourceAsStream("/" + name)) {
                new org.yaml.snakeyaml.Yaml(options).load(stream);
            }
        }
    }

    @Test
    void shippedMenusDefineEveryMenu() throws Exception {
        YamlConfiguration menus = shipped("menus.yml");
        for (String menu : new String[] {"no-team", "main", "members", "member", "invite-picker", "invites", "sent-invites",
                "requests", "homes", "bank", "level", "relations", "settings", "color", "top", "browse", "profile", "confirm"}) {
            assertTrue(menus.contains(menu + ".title"), menu);
        }
    }
}
