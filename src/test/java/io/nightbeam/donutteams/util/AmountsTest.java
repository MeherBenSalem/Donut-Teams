package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AmountsTest {

    @Test
    void parsesSuffixesSeparatorsAndRejectsJunk() {
        assertEquals(500, Amounts.parse("500"));
        assertEquals(2500, Amounts.parse("2.5k"));
        assertEquals(1_000_000, Amounts.parse("1M"));
        assertEquals(1234.5, Amounts.parse("$1,234.50"));
        assertEquals(0.01, Amounts.parse("0.019"));
        assertEquals(-1, Amounts.parse("-5"));
        assertEquals(-1, Amounts.parse("0"));
        assertEquals(-1, Amounts.parse("abc"));
        assertEquals(-1, Amounts.parse(""));
        assertEquals(-1, Amounts.parse("NaN"));
    }

    @Test
    void shortAndFullForms() {
        assertEquals("950", Amounts.shortForm(950));
        assertEquals("12.5k", Amounts.shortForm(12_500));
        assertEquals("1m", Amounts.shortForm(1_000_000));
        assertEquals("2.4b", Amounts.shortForm(2_490_000_000D));
        assertEquals("12,500", Amounts.full(12_500));
        assertEquals("12,500.5", Amounts.full(12_500.5));
    }

    @Test
    void durations() {
        assertEquals("4m 12s", Durations.compact(252_000));
        assertEquals("3d 2h", Durations.compact((3 * 86_400 + 2 * 3600) * 1000L));
        assertEquals("now", Durations.ago(1000, 30_000));
        assertEquals("5m", Durations.ago(0, 5 * 60_000));
    }
}
