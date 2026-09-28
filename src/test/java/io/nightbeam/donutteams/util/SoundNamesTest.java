package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SoundNamesTest {

    @Test
    void registryCandidatesPreferNamespacedThenLegacyEnumShape() {
        assertEquals(List.of("minecraft:entity.enderman.teleport", "minecraft:entity_enderman_teleport"),
                SoundNames.keyCandidates("ENTITY_ENDERMAN_TELEPORT"));
        assertEquals(List.of("minecraft:block.note_block.hat"),
                SoundNames.keyCandidates("minecraft:block.note_block.hat"));
        assertEquals("ENTITY_ENDERMAN_TELEPORT", SoundNames.enumName("entity.enderman.teleport"));
        assertEquals("BLOCK_NOTE_BLOCK_HAT", SoundNames.enumName("minecraft:block.note_block.hat"));
    }
}
