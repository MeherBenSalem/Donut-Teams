package io.nightbeam.donutteams.model;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Team {

    private final UUID id;
    private final String name;
    private final String tag;
    private volatile UUID ownerId;
    private final long createdAtMillis;
    private final Map<UUID, TeamMember> members = new ConcurrentHashMap<>();
    private volatile TeamHome home;
    private volatile TeamSettings settings;

    public Team(UUID id, String name, String tag, UUID ownerId, long createdAtMillis) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerId = ownerId;
        this.createdAtMillis = createdAtMillis;
        this.settings = TeamSettings.defaults(id, false);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String tag() {
        return tag;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public void ownerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public Collection<TeamMember> members() {
        return members.values();
    }

    public TeamMember member(UUID playerId) {
        return members.get(playerId);
    }

    public void putMember(TeamMember member) {
        members.put(member.playerId(), member);
    }

    public TeamMember removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    public int size() {
        return members.size();
    }

    public TeamHome home() {
        return home;
    }

    public void home(TeamHome home) {
        this.home = home;
    }

    public TeamSettings settings() {
        return settings;
    }

    public void settings(TeamSettings settings) {
        this.settings = settings;
    }

    public boolean friendlyFire() {
        return settings != null && settings.friendlyFire();
    }
}
