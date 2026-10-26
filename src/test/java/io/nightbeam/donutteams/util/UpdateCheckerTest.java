package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nightbeam.donutteams.hook.UpdateChecker;
import org.junit.jupiter.api.Test;

class UpdateCheckerTest {

    @Test
    void comparesVersionNumbers() {
        assertTrue(UpdateChecker.newer("2.0.1", "2.0.0"));
        assertTrue(UpdateChecker.newer("2.1", "2.0.9"));
        assertFalse(UpdateChecker.newer("2.0.0", "2.0.0"));
        assertFalse(UpdateChecker.newer("1.9.9", "2.0.0"));
        assertFalse(UpdateChecker.newer("2.0.0-beta", "2.0.0"));
    }
}
