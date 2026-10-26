package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void legacyCodesBecomeMiniMessage() {
        assertEquals("<reset><green>Hello <bold>you", Text.legacyToMini("&aHello &lyou"));
        assertEquals("<reset><#ff8800>Orange", Text.legacyToMini("&#FF8800Orange"));
        assertEquals("<reset>plain", Text.legacyToMini("§rplain"));
        assertEquals("<gold>Already mini", Text.legacyToMini("<gold>Already mini"));
    }

    @Test
    void tagFromNameKeepsLettersAndDigits() {
        assertEquals("MyTea", TeamNameValidator.tagFromName("My_Team", 5));
        assertEquals("NB2", TeamNameValidator.tagFromName("NB_2", 6));
    }

    @Test
    void blockedWordsIgnoreCaseAndUnderscores() {
        assertEquals(true, TeamNameValidator.blocked("The_Adm_in", java.util.List.of("admin")));
        assertEquals(false, TeamNameValidator.blocked("Nightbeam", java.util.List.of("admin")));
    }
}
