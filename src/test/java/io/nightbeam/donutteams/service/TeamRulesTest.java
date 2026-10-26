package io.nightbeam.donutteams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.nightbeam.donutteams.model.TeamRole;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeamRulesTest {

    @Test
    void createAllowsValidNewTeam() {
        TeamRules.PreparedName prepared = TeamRules.prepareCreate("Nightbeam", "NB", 6);
        assertEquals("Nightbeam", prepared.name());
        assertEquals("NB", prepared.tag());
        assertNull(TeamRules.createDenial(true, false, prepared, false, 3, 16, 2, 6));
    }

    @Test
    void tagComesFromTheNameWithoutUnderscoresAndIsCut() {
        TeamRules.PreparedName fromName = TeamRules.prepareCreate("My_Team", "  ", 6);
        assertEquals("MyTeam", fromName.tag());
        assertNull(TeamRules.createDenial(true, false, fromName, false, 3, 16, 2, 6),
                "1.x refused a name with an underscore and no tag; 2.0 makes a valid tag from it");
        TeamRules.PreparedName truncated = TeamRules.prepareCreate("Alpha", "TOOLONGTAG", 6);
        assertEquals("TOOLON", truncated.tag());
    }

    @Test
    void createRejectsPermissionNameCollisionBlockedAndInvalidInput() {
        TeamRules.PreparedName prepared = TeamRules.prepareCreate("Nightbeam", "NB", 6);
        assertEquals(TeamRules.Denial.NO_CREATE_PERMISSION, TeamRules.createDenial(false, false, prepared, false, 3, 16, 2, 6));
        assertEquals(TeamRules.Denial.ALREADY_IN_TEAM, TeamRules.createDenial(true, true, prepared, false, 3, 16, 2, 6));
        assertEquals(TeamRules.Denial.INVALID_NAME,
                TeamRules.createDenial(true, false, TeamRules.prepareCreate("ab", "NB", 6), false, 3, 16, 2, 6));
        assertEquals(TeamRules.Denial.INVALID_TAG,
                TeamRules.createDenial(true, false, TeamRules.prepareCreate("Nightbeam", "x", 6), false, 3, 16, 2, 6));
        assertEquals(TeamRules.Denial.NAME_TAKEN, TeamRules.createDenial(true, false, prepared, true, 3, 16, 2, 6));
        assertEquals(TeamRules.Denial.BLOCKED_NAME, TeamRules.createDenial(true, false,
                TeamRules.prepareCreate("TheAdmins", "TA", 6), false, 3, 16, 2, 6, List.of("admin")));
    }

    @Test
    void inviteRules() {
        assertNull(TeamRules.inviteDenial(true, true, false, false, 3, 8));
        assertEquals(TeamRules.Denial.NOT_IN_TEAM, TeamRules.inviteDenial(false, true, false, false, 1, 8));
        assertEquals(TeamRules.Denial.NO_INVITE_PERMISSION, TeamRules.inviteDenial(true, false, false, false, 1, 8));
        assertEquals(TeamRules.Denial.INVITE_SELF, TeamRules.inviteDenial(true, true, true, false, 1, 8));
        assertEquals(TeamRules.Denial.ALREADY_INVITED_OR_MEMBER, TeamRules.inviteDenial(true, true, false, true, 1, 8));
        assertEquals(TeamRules.Denial.TEAM_FULL, TeamRules.inviteDenial(true, true, false, false, 8, 8));
    }

    @Test
    void joinNeedsAnInviteUnlessTheTeamIsOpen() {
        assertNull(TeamRules.joinDenial(false, true, true, false, 4, 8));
        assertNull(TeamRules.joinDenial(false, true, false, false, true, 4, 8));
        assertEquals(TeamRules.Denial.ALREADY_IN_TEAM, TeamRules.joinDenial(true, true, true, false, 4, 8));
        assertEquals(TeamRules.Denial.TEAM_NOT_FOUND, TeamRules.joinDenial(false, false, false, false, 0, 8));
        assertEquals(TeamRules.Denial.NO_INVITE, TeamRules.joinDenial(false, true, false, false, 4, 8));
        assertEquals(TeamRules.Denial.INVITE_EXPIRED, TeamRules.joinDenial(false, true, true, true, 4, 8));
        assertEquals(TeamRules.Denial.TEAM_FULL, TeamRules.joinDenial(false, true, false, false, true, 8, 8));
    }

    @Test
    void ownerCannotLeaveButMemberCan() {
        assertNull(TeamRules.leaveDenial(true, false));
        assertEquals(TeamRules.Denial.NOT_IN_TEAM, TeamRules.leaveDenial(false, false));
        assertEquals(TeamRules.Denial.OWNER_CANNOT_LEAVE, TeamRules.leaveDenial(true, true));
    }

    @Test
    void onlyHigherRanksManageLowerOnes() {
        assertNull(TeamRules.manageDenial(TeamRole.OWNER, TeamRole.OFFICER, false));
        assertNull(TeamRules.manageDenial(TeamRole.OFFICER, TeamRole.MEMBER, false));
        assertEquals(TeamRules.Denial.RANK_TOO_LOW, TeamRules.manageDenial(TeamRole.OFFICER, TeamRole.OFFICER, false));
        assertEquals(TeamRules.Denial.RANK_TOO_LOW, TeamRules.manageDenial(TeamRole.MEMBER, TeamRole.OWNER, false));
        assertEquals(TeamRules.Denial.CANNOT_MANAGE_SELF, TeamRules.manageDenial(TeamRole.OWNER, TeamRole.OWNER, true));
        assertEquals(TeamRules.Denial.NOT_A_MEMBER, TeamRules.manageDenial(TeamRole.OWNER, null, false));
        assertEquals(TeamRole.OFFICER, TeamRules.promoted(TeamRole.MEMBER));
        assertNull(TeamRules.promoted(TeamRole.OFFICER));
        assertEquals(TeamRole.MEMBER, TeamRules.demoted(TeamRole.OFFICER));
        assertNull(TeamRules.demoted(TeamRole.MEMBER));
    }
}
