package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SlotPermissionParserTest {

    @Test
    void findsTheHighestSlotNode() {
        assertEquals(16, SlotPermissionParser.highest(Set.of("donutteams.use", "donutteams.slots.4", "donutteams.slots.16")));
        assertEquals(0, SlotPermissionParser.highest(Set.of("donutteams.use")));
    }

    @Test
    void capRaisesTheLevelCapButNeverPassesTheHardCap() {
        assertEquals(8, SlotPermissionParser.cap(8, 0, 50));
        assertEquals(16, SlotPermissionParser.cap(8, 16, 50));
        assertEquals(10, SlotPermissionParser.cap(10, 4, 50));
        assertEquals(50, SlotPermissionParser.cap(8, 500, 50));
    }

    @Test
    void ignoresMalformedNodes() {
        assertTrue(SlotPermissionParser.isSlotNode("donutteams.slots.8"));
        assertFalse(SlotPermissionParser.isSlotNode("donutteams.slots.unlimited"));
        assertEquals(0, SlotPermissionParser.highest(Set.of("donutteams.slots.abc")));
    }
}
