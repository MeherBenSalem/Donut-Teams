package io.nightbeam.donutteams.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClickLimiterTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void clicksFasterThanTheCooldownAreDropped() {
        ClickLimiter limiter = new ClickLimiter();
        assertTrue(limiter.accept(alice, 1_000, 150));
        assertFalse(limiter.accept(alice, 1_050, 150));
        assertFalse(limiter.accept(alice, 1_149, 150));
        assertTrue(limiter.accept(alice, 1_150, 150));
    }

    @Test
    void anAutoclickerGetsAboutSevenClicksASecond() {
        ClickLimiter limiter = new ClickLimiter();
        int accepted = 0;
        for (long t = 0; t < 1_000; t += 10) {        // 100 clicks in one second
            if (limiter.accept(alice, t, 150)) {
                accepted++;
            }
        }
        assertTrue(accepted <= 7, "accepted " + accepted);
    }

    @Test
    void playersAreLimitedSeparately() {
        ClickLimiter limiter = new ClickLimiter();
        assertTrue(limiter.accept(alice, 1_000, 150));
        assertTrue(limiter.accept(bob, 1_010, 150));
    }

    @Test
    void zeroTurnsTheLimitOff() {
        ClickLimiter limiter = new ClickLimiter();
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.accept(alice, 1_000, 0));
        }
    }

    @Test
    void aClockThatGoesBackwardsNeverLocksAPlayerOut() {
        ClickLimiter limiter = new ClickLimiter();
        assertTrue(limiter.accept(alice, 10_000, 150));
        assertTrue(limiter.accept(alice, 5_000, 150));
    }

    @Test
    void forgettingAPlayerResetsTheirCooldown() {
        ClickLimiter limiter = new ClickLimiter();
        assertTrue(limiter.accept(alice, 1_000, 150));
        limiter.forget(alice);
        assertTrue(limiter.accept(alice, 1_010, 150));
    }
}
