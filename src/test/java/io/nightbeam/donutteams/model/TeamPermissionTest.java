package io.nightbeam.donutteams.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeamPermissionTest {

    @Test
    void oneDotXDefaultIsSpeakOnly() {
        assertEquals(EnumSet.of(TeamPermission.SPEAK), TeamPermission.defaultMember());
    }

    @Test
    void serializeRoundTripAndUnknownNamesAreIgnored() {
        EnumSet<TeamPermission> original = EnumSet.of(TeamPermission.INVITE, TeamPermission.BANK_WITHDRAW, TeamPermission.SPEAK);
        String raw = TeamPermission.serialize(original);
        assertEquals(original, TeamPermission.deserialize(raw));
        assertEquals(EnumSet.of(TeamPermission.HOME), TeamPermission.deserialize("HOME,FLY,,"));
    }

    @Test
    void configNamesAcceptDashesAndStar() {
        assertEquals(TeamPermission.BANK_DEPOSIT, TeamPermission.parse("bank-deposit"));
        assertNull(TeamPermission.parse("nope"));
        assertEquals(TeamPermission.all(), TeamPermission.parseAll(List.of("*")));
    }

    @Test
    void rolesAndColours() {
        assertEquals(TeamRole.OFFICER, TeamRole.parse("admin"));
        assertEquals(TeamRole.MEMBER, TeamRole.parse("???"));
        assertTrue(TeamRole.OWNER.outranks(TeamRole.OFFICER));
        assertEquals(TeamColor.DARK_PURPLE, TeamColor.parse("purple", null));
        assertEquals("Light Purple", TeamColor.LIGHT_PURPLE.displayName());
    }

    @Test
    void teamTotalsAndDefaultHome() {
        Team team = new Team(java.util.UUID.randomUUID(), "A", "A", java.util.UUID.randomUUID(), 0);
        team.putMember(new TeamMember(java.util.UUID.randomUUID(), "x", TeamRole.MEMBER, null, 0, 0, 4, 1));
        team.putMember(new TeamMember(java.util.UUID.randomUUID(), "y", TeamRole.MEMBER, null, 0, 0, 2, 3));
        assertEquals(6, team.kills());
        assertEquals(1.5, team.kdr());
        team.putHome(new TeamHome(team.id(), "farm", "w", 0, 0, 0, 0, 0, null, 5));
        team.putHome(new TeamHome(team.id(), "home", "w", 0, 0, 0, 0, 0, null, 9));
        assertEquals("home", team.defaultHome().name());
        assertEquals("home", team.homeList().get(0).name());
    }
}
