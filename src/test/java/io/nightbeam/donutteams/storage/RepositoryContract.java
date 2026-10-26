package io.nightbeam.donutteams.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nightbeam.donutteams.model.BankEntry;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamColor;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import java.util.List;
import java.util.UUID;

/** The same checks for every database: what is saved loads back the same. */
final class RepositoryContract {

    private RepositoryContract() {
    }

    static void run(TeamRepository repository) throws Exception {
        long now = System.currentTimeMillis();
        UUID ownerId = UUID.randomUUID();
        Team team = new Team(UUID.randomUUID(), "Alpha_" + (now % 10000), "ALP", ownerId, now);
        TeamMember owner = new TeamMember(ownerId, "Moonbeam", TeamRole.OWNER, TeamPermission.all(), now);
        team.putMember(owner);
        team.color(TeamColor.LIGHT_PURPLE);
        team.description("Night owls");
        team.balance(1234.5);
        team.level(3);
        team.open(true);
        repository.insertTeam(team, owner);

        TeamMember officer = new TeamMember(UUID.randomUUID(), "Kiri", TeamRole.OFFICER, java.util.EnumSet.of(TeamPermission.INVITE), now, now, 7, 2);
        repository.saveMember(team.id(), officer);
        repository.saveHome(new TeamHome(team.id(), "Base", "world", 1, 2, 3, 4, 5, ownerId, now));
        repository.saveHome(new TeamHome(team.id(), "farm", "world_nether", 6, 7, 8, 0, 0, null, now));

        Team other = new Team(UUID.randomUUID(), "Bravo_" + (now % 10000), "BRV", UUID.randomUUID(), now);
        TeamMember otherOwner = new TeamMember(other.ownerId(), "Rook", TeamRole.OWNER, TeamPermission.all(), now);
        other.putMember(otherOwner);
        repository.insertTeam(other, otherOwner);
        repository.saveRelation(team.id(), other.id(), "ALLY");
        repository.saveRelation(other.id(), team.id(), "ALLY");

        repository.saveInvite(new TeamInvite(team.id(), UUID.randomUUID(), ownerId, now + 60_000));
        repository.saveInvite(new TeamInvite(team.id(), UUID.randomUUID(), ownerId, now - 60_000));

        List<Team> loaded = repository.loadAll();
        Team back = loaded.stream().filter(t -> t.id().equals(team.id())).findFirst().orElseThrow();
        assertEquals(TeamColor.LIGHT_PURPLE, back.color());
        assertEquals("Night owls", back.description());
        assertEquals(1234.5, back.balance());
        assertEquals(3, back.level());
        assertTrue(back.open());
        assertEquals(2, back.size());
        assertEquals(7, back.member(officer.playerId()).kills());
        assertEquals(TeamRole.OFFICER, back.member(officer.playerId()).role());
        assertEquals(2, back.homes().size());
        assertEquals("world_nether", back.home("FARM").world());
        assertTrue(back.alliedWith(other.id()));
        assertEquals(1, repository.loadInvites().stream().filter(i -> i.teamId().equals(team.id())).count());

        repository.saveChest(team.id(), "0:abc;5:def");
        assertEquals("0:abc;5:def", repository.loadChest(team.id()));

        for (int i = 1; i <= 5; i++) {
            repository.insertBankEntry(new BankEntry(team.id(), ownerId, "Moonbeam", BankEntry.Type.DEPOSIT, i * 100, i * 100, now + i), 3);
        }
        List<BankEntry> history = repository.loadBankHistory(team.id(), 10);
        assertEquals(3, history.size(), "only the newest lines are kept");
        assertEquals(500, history.get(0).amount());

        repository.deleteHome(team.id(), "base");
        repository.saveRelation(team.id(), other.id(), null);
        repository.deleteTeam(other.id());
        List<Team> after = repository.loadAll();
        Team afterTeam = after.stream().filter(t -> t.id().equals(team.id())).findFirst().orElseThrow();
        assertNull(afterTeam.home("base"));
        assertFalse(afterTeam.alliedWith(other.id()));
        assertTrue(after.stream().noneMatch(t -> t.id().equals(other.id())));

        repository.deleteTeam(team.id());
        assertEquals("", repository.loadChest(team.id()));
        assertTrue(repository.loadBankHistory(team.id(), 10).isEmpty());
    }
}
