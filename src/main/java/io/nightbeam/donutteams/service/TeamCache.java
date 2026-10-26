package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.util.TeamNameValidator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Teams in memory, found by id, name or member. Safe to read from any thread. */
public final class TeamCache {

    private final Map<UUID, Team> byId = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToTeam = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToTeam = new ConcurrentHashMap<>();
    private final Map<String, TeamInvite> invites = new ConcurrentHashMap<>();

    public void replaceAll(Collection<Team> teams, Collection<TeamInvite> loadedInvites) {
        byId.clear();
        playerToTeam.clear();
        nameToTeam.clear();
        invites.clear();
        for (Team team : teams) {
            put(team);
        }
        long now = System.currentTimeMillis();
        for (TeamInvite invite : loadedInvites) {
            if (!invite.expired(now) && byId.containsKey(invite.teamId())) {
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
        byId.remove(team.id());
        nameToTeam.remove(TeamNameValidator.nameKey(team.name()), team.id());
        team.members().forEach(member -> playerToTeam.remove(member.playerId(), team.id()));
        invites.entrySet().removeIf(entry -> entry.getValue().teamId().equals(team.id()));
    }

    public void rename(Team team, String oldName) {
        nameToTeam.remove(TeamNameValidator.nameKey(oldName), team.id());
        nameToTeam.put(TeamNameValidator.nameKey(team.name()), team.id());
    }

    public Team byId(UUID teamId) {
        return teamId == null ? null : byId.get(teamId);
    }

    public Team byPlayer(UUID playerId) {
        UUID teamId = playerId == null ? null : playerToTeam.get(playerId);
        return byId(teamId);
    }

    public Team byName(String name) {
        if (name == null) {
            return null;
        }
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

    /** The invite, or null when there is none or it has expired. */
    public TeamInvite invite(UUID teamId, UUID playerId) {
        TeamInvite invite = invites.get(inviteKey(teamId, playerId));
        if (invite != null && invite.expired()) {
            invites.remove(inviteKey(teamId, playerId), invite);
            return null;
        }
        return invite;
    }

    public void removeInvite(UUID teamId, UUID playerId) {
        invites.remove(inviteKey(teamId, playerId));
    }

    public void removeInvitesFor(UUID playerId) {
        invites.values().removeIf(invite -> invite.playerId().equals(playerId));
    }

    /** Live invites a player has received, newest first. */
    public List<TeamInvite> invitesFor(UUID playerId) {
        long now = System.currentTimeMillis();
        List<TeamInvite> out = new ArrayList<>();
        for (TeamInvite invite : invites.values()) {
            if (invite.playerId().equals(playerId) && !invite.expired(now) && byId.containsKey(invite.teamId())) {
                out.add(invite);
            }
        }
        out.sort((a, b) -> Long.compare(b.expiresAtMillis(), a.expiresAtMillis()));
        return out;
    }

    /** Live invites a team has sent. */
    public List<TeamInvite> teamInvites(UUID teamId) {
        long now = System.currentTimeMillis();
        List<TeamInvite> out = new ArrayList<>();
        for (TeamInvite invite : invites.values()) {
            if (invite.teamId().equals(teamId) && !invite.expired(now)) {
                out.add(invite);
            }
        }
        return out;
    }

    /** Drops expired invites; returns how many. */
    public int purgeExpiredInvites() {
        long now = System.currentTimeMillis();
        int before = invites.size();
        invites.values().removeIf(invite -> invite.expired(now));
        return before - invites.size();
    }

    public Collection<Team> teams() {
        return byId.values();
    }

    public int size() {
        return byId.size();
    }

    private static String inviteKey(UUID teamId, UUID playerId) {
        return teamId + ":" + playerId;
    }
}
