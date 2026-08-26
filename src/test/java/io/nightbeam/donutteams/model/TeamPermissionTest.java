package io.nightbeam.donutteams.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class TeamPermissionTest {

    @Test
    void defaultMemberIsSpeakOnly() {
        EnumSet<TeamPermission> defaults = TeamPermission.defaultMember();
        assertEquals(EnumSet.of(TeamPermission.SPEAK), defaults);
        assertFalse(defaults.contains(TeamPermission.INVITE));
        assertFalse(defaults.contains(TeamPermission.KICK));
        assertFalse(defaults.contains(TeamPermission.HOME));
        assertFalse(defaults.contains(TeamPermission.SETHOME));
        assertFalse(defaults.contains(TeamPermission.PVP));
    }

    @Test
    void serializeRoundTrip() {
        EnumSet<TeamPermission> original = EnumSet.of(TeamPermission.INVITE, TeamPermission.HOME, TeamPermission.SPEAK);
        String raw = TeamPermission.serialize(original);
        assertEquals(original, TeamPermission.deserialize(raw));
        assertTrue(raw.contains("INVITE"));
    }
}
