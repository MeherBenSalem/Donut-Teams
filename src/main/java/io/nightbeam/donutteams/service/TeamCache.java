package io.nightbeam.donutteams.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.util.TeamNameValidator;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class TeamCache {

    private final Cache<UUID, Team> byId = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();
    private final Map<UUID, UUID> playerToTeam = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToTeam = new ConcurrentHashMap<>();
    private final Map<String, TeamInvite> invites = new ConcurrentHashMap<>();

    public void replaceAll(Collection<Team> teams, Collection<TeamInvite> loadedInvites) {
        byId.invalidateAll();
        playerToTeam.clear();
        nameToTeam.clear();
        invites.clear();
        for (Team team : teams) {
            put(team);
        }
        long now = System.currentTimeMillis();
        for (TeamInvite invite : loadedInvites) {
            if (!invite.expired() && invite.expiresAtMillis() > now) {
                invites.put(inviteKey(invite.teamId(), invite.playerId()), invite);
            }
        }
    }

    public void put(Team team) {
        byId.put(team.id(), team);
        nameToTeam.put(TeamNameValidator.nameKey(team.name()), team.id());
        team.members().forEach(member -> playerToTeam.put(member.playerId(), team.id()));
    }

    public void remove(Team team) {
        byId.invalidate(team.id());
        nameToTeam.remove(TeamNameValidator.nameKey(team.name()));
        team.members().forEach(member -> playerToTeam.remove(member.playerId(), team.id()));
        invites.entrySet().removeIf(entry -> entry.getValue().teamId().equals(team.id()));
    }

    public Team byId(UUID teamId) {
        return teamId == null ? null : byId.getIfPresent(teamId);
    }

    public Team byPlayer(UUID playerId) {
        UUID teamId = playerToTeam.get(playerId);
        return byId(teamId);
    }

    public Team byName(String name) {
        UUID teamId = nameToTeam.get(TeamNameValidator.nameKey(name));
        return byId(teamId);
    }

    public void indexPlayer(UUID playerId, UUID teamId) {
        playerToTeam.put(playerId, teamId);
    }

    public void unindexPlayer(UUID playerId) {
        playerToTeam.remove(playerId);
    }

    public void putInvite(TeamInvite invite) {
        invites.put(inviteKey(invite.teamId(), invite.playerId()), invite);
    }

    public TeamInvite invite(UUID teamId, UUID playerId) {
        return invites.get(inviteKey(teamId, playerId));
    }

    public void removeInvite(UUID teamId, UUID playerId) {
        invites.remove(inviteKey(teamId, playerId));
    }

    public Collection<TeamInvite> invitesFor(UUID playerId) {
        return invites.values().stream().filter(invite -> invite.playerId().equals(playerId)).toList();
    }

    public Collection<TeamInvite> teamInvites(UUID teamId) {
        return invites.values().stream().filter(invite -> invite.teamId().equals(teamId)).toList();
    }

    public Collection<Team> teams() {
        return byId.asMap().values();
    }

    private static String inviteKey(UUID teamId, UUID playerId) {
        return teamId + ":" + playerId;
    }
}
