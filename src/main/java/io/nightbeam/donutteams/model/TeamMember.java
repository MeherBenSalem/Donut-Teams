package io.nightbeam.donutteams.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A member of a team. Fields that change during play are volatile or atomic; the team service changes them under its
 * write lock, and any thread may read them.
 */
public final class TeamMember {

    private final UUID playerId;
    private volatile String name;
    private volatile TeamRole role;
    private volatile Set<TeamPermission> permissions;
    private final long joinedAtMillis;
    private volatile long lastSeenMillis;
    private final AtomicInteger kills;
    private final AtomicInteger deaths;

    public TeamMember(UUID playerId, String name, TeamRole role, Set<TeamPermission> permissions, long joinedAtMillis) {
        this(playerId, name, role, permissions, joinedAtMillis, joinedAtMillis, 0, 0);
    }

    public TeamMember(UUID playerId, String name, TeamRole role, Set<TeamPermission> permissions, long joinedAtMillis,
                      long lastSeenMillis, int kills, int deaths) {
        this.playerId = playerId;
        this.name = name == null ? playerId.toString().substring(0, 8) : name;
        this.role = role == null ? TeamRole.MEMBER : role;
        this.permissions = copy(permissions);
        this.joinedAtMillis = joinedAtMillis;
        this.lastSeenMillis = lastSeenMillis;
        this.kills = new AtomicInteger(Math.max(0, kills));
        this.deaths = new AtomicInteger(Math.max(0, deaths));
    }

    public UUID playerId() {
        return playerId;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public TeamRole role() {
        return role;
    }

    public void role(TeamRole role) {
        this.role = role;
    }

    /** The stored permissions (owners have every permission regardless). */
    public Set<TeamPermission> permissions() {
        return permissions;
    }

    public void permissions(Set<TeamPermission> next) {
        this.permissions = copy(next);
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

    public long lastSeenMillis() {
        return lastSeenMillis;
    }

    public void lastSeenMillis(long value) {
        this.lastSeenMillis = value;
    }

    public int kills() {
        return kills.get();
    }

    public int deaths() {
        return deaths.get();
    }

    public int addKill() {
        return kills.incrementAndGet();
    }

    public int addDeath() {
        return deaths.incrementAndGet();
    }

    public void resetStats() {
        kills.set(0);
        deaths.set(0);
    }

    private static Set<TeamPermission> copy(Set<TeamPermission> source) {
        EnumSet<TeamPermission> set = source == null || source.isEmpty()
                ? EnumSet.noneOf(TeamPermission.class)
                : EnumSet.copyOf(source);
        return Collections.unmodifiableSet(set);
    }
}
