package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SlotPermissionParserTest {

    @Test
    void parsesHighestSlotAndAppliesLiteCap() {
        int value = SlotPermissionParser.parseHighest(
                Set.of("donutteams.use", "donutteams.slots.4", "donutteams.slots.8", "donutteams.slots.16"),
                8,
                8);
        assertEquals(8, value);
    }

    @Test
    void usesDefaultWhenNoSlotNodes() {
        assertEquals(8, SlotPermissionParser.parseHighest(Set.of("donutteams.use"), 8, 8));
    }

    @Test
    void ignoresMalformedNodes() {
        assertTrue(SlotPermissionParser.isSlotNode("donutteams.slots.8"));
        assertFalse(SlotPermissionParser.isSlotNode("donutteams.slots.unlimited"));
        assertEquals(8, SlotPermissionParser.parseHighest(Set.of("donutteams.slots.abc"), 8, 8));
    }
}
