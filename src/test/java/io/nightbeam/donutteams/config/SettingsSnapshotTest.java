package io.nightbeam.donutteams.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class SettingsSnapshotTest {

    @Test
    void shippedConfigMatchesLiteDefaults() throws Exception {
        try (InputStream stream = SettingsSnapshotTest.class.getResourceAsStream("/config.yml")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = new Yaml().load(stream);
            SettingsSnapshot settings = SettingsSnapshot.fromMap(root);
            assertEquals("teams.db", settings.sqliteFile());
            assertEquals(8, settings.defaultMaxMembers());
            assertEquals(8, settings.liteMaxMembers());
            assertEquals(3, settings.nameMin());
            assertEquals(16, settings.nameMax());
            assertEquals(2, settings.tagMin());
            assertEquals(6, settings.tagMax());
            assertEquals(300, settings.inviteExpireSeconds());
            assertEquals(3, settings.homeWarmupSeconds());
            assertTrue(settings.cancelHomeOnMove());
            assertTrue(settings.chatEnabled());
            assertFalse(settings.defaultFriendlyFire());
            assertTrue(settings.metricsEnabled());
            assertEquals(0, settings.bstatsId());
        }
    }

    @Test
    void missingKeysFallBackToDefaults() {
        SettingsSnapshot settings = SettingsSnapshot.fromMap(Map.of());
        assertEquals("teams.db", settings.sqliteFile());
        assertEquals(8, settings.defaultMaxMembers());
        assertEquals(8, settings.liteMaxMembers());
        assertEquals(3, settings.nameMin());
        assertEquals(16, settings.nameMax());
        assertEquals(300, settings.inviteExpireSeconds());
        assertTrue(settings.cancelHomeOnMove());
        assertTrue(settings.chatEnabled());
        assertFalse(settings.defaultFriendlyFire());
        assertEquals(0, settings.bstatsId());
    }

    @Test
    void nestedOverridesAreReadFromDottedPaths() {
        Map<String, Object> root = Map.of(
                "teams", Map.of(
                        "default-max-members", 4,
                        "lite-max-members", 6,
                        "invite-expire-seconds", "120"),
                "chat", Map.of("enabled", "off"),
                "pvp", Map.of("default-friendly-fire", true),
                "metrics", Map.of("enabled", false, "bstats-id", 33560));
        SettingsSnapshot settings = SettingsSnapshot.fromMap(root);
        assertEquals(4, settings.defaultMaxMembers());
        assertEquals(6, settings.liteMaxMembers());
        assertEquals(120, settings.inviteExpireSeconds());
        assertFalse(settings.chatEnabled());
        assertTrue(settings.defaultFriendlyFire());
        assertFalse(settings.metricsEnabled());
        assertEquals(33560, settings.bstatsId());
    }
}
