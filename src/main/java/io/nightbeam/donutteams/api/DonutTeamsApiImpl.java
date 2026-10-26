package io.nightbeam.donutteams.api;

import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.service.TeamService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DonutTeamsApiImpl implements DonutTeamsApi {

    private final TeamService teams;

    public DonutTeamsApiImpl(TeamService teams) {
        this.teams = teams;
    }

    @Override
    public Optional<TeamSnapshot> teamByPlayer(UUID playerId) {
        return teams.snapshot(playerId);
    }

    @Override
    public Optional<TeamSnapshot> teamById(UUID teamId) {
        return teams.snapshotById(teamId);
    }

    @Override
    public Optional<TeamSnapshot> teamByName(String name) {
        return teams.snapshotByName(name);
    }

    @Override
    public boolean teammates(UUID first, UUID second) {
        return teams.teammates(first, second);
    }

    @Override
    public boolean friendlyFire(UUID teamId) {
        Team team = teams.teamById(teamId);
        return team != null && team.friendlyFire();
    }

    @Override
    public Relation relation(UUID viewer, UUID target) {
        return teams.relation(viewer, target);
    }

    @Override
    public Optional<TeamRole> role(UUID playerId) {
        Team team = teams.teamByPlayer(playerId);
        TeamMember member = team == null ? null : team.member(playerId);
        return member == null ? Optional.empty() : Optional.of(member.role());
    }

    @Override
    public Collection<TeamSnapshot> teams() {
        List<TeamSnapshot> out = new java.util.ArrayList<>();
        for (Team team : teams.cache().teams()) {
            out.add(teams.toSnapshot(team));
        }
        return out;
    }
}
