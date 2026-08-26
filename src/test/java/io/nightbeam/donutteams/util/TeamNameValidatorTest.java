package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamNameValidatorTest {

    @Test
    void acceptsValidNamesAndTags() {
        assertTrue(TeamNameValidator.validName("Nightbeam", 3, 16));
        assertTrue(TeamNameValidator.validName("A_1", 3, 16));
        assertTrue(TeamNameValidator.validTag("NB", 2, 6));
        assertTrue(TeamNameValidator.validTag("Donut", 2, 6));
    }

    @Test
    void rejectsInvalidNamesAndTags() {
        assertFalse(TeamNameValidator.validName("ab", 3, 16));
        assertFalse(TeamNameValidator.validName("spaces not ok", 3, 16));
        assertFalse(TeamNameValidator.validTag("x", 2, 6));
        assertFalse(TeamNameValidator.validTag("toolongtag", 2, 6));
        assertFalse(TeamNameValidator.validTag("n b", 2, 6));
    }
}
