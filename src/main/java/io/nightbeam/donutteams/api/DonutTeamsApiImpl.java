package io.nightbeam.donutteams.api;

import io.nightbeam.donutteams.service.TeamService;
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
        var team = teams.teamById(teamId);
        return team != null && team.friendlyFire();
    }
}
