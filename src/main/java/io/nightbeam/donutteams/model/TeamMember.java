package io.nightbeam.donutteams.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class TeamMember {

    private final UUID playerId;
    private volatile String name;
    private final TeamRole role;
    private final EnumSet<TeamPermission> permissions;
    private final long joinedAtMillis;

    public TeamMember(UUID playerId, String name, TeamRole role, Set<TeamPermission> permissions, long joinedAtMillis) {
        this.playerId = playerId;
        this.name = name;
        this.role = role;
        this.permissions = permissions == null || permissions.isEmpty()
                ? EnumSet.noneOf(TeamPermission.class)
                : EnumSet.copyOf(permissions);
        this.joinedAtMillis = joinedAtMillis;
    }

    public UUID playerId() {
        return playerId;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public TeamRole role() {
        return role;
    }

    public Set<TeamPermission> permissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public boolean has(TeamPermission permission) {
        return role == TeamRole.OWNER || permissions.contains(permission);
    }

    public boolean isOwner() {
        return role == TeamRole.OWNER;
    }

    public long joinedAtMillis() {
        return joinedAtMillis;
    }

    public TeamMember withPermissions(Set<TeamPermission> next) {
        return new TeamMember(playerId, name, role, next, joinedAtMillis);
    }

    public TeamMember withRole(TeamRole nextRole, Set<TeamPermission> nextPermissions) {
        return new TeamMember(playerId, name, nextRole, nextPermissions, joinedAtMillis);
    }
}
