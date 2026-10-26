package io.nightbeam.donutteams.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nightbeam.donutteams.gui.MenuConfig;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MenuConfigTest {

    @Test
    void rangesAndSingleSlotsAreRead() throws Exception {
        MenuConfig config = new MenuConfig();
        try (var reader = new InputStreamReader(MenuConfigTest.class.getResourceAsStream("/menus.yml"), StandardCharsets.UTF_8)) {
            config.load(YamlConfiguration.loadConfiguration(reader));
        }
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34),
                config.content("members"));
        assertEquals(4, config.item("main", "info").slot());
        assertEquals(List.of(10, 11, 12), config.item("confirm", "confirm").slots());
        // Shared items take their look from "common" and their slot from the menu.
        assertEquals("ARROW", config.item("members", "next").material());
        assertEquals(53, config.item("members", "next").slot());
        assertEquals(12, config.item("member", "permission").slots().size());
    }
}
