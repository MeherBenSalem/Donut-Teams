package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.api.event.TeamRelationChangeEvent;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.model.AllyRequest;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.storage.DataWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allies (both teams agree: one asks, the other accepts) and enemies (one team declares, no consent needed).
 * Ally requests live in memory and expire.
 */
public final class RelationService {

    private final Supplier<Settings> settings;
    private final Messages messages;
    private final TeamService teams;
    private final DataWriter writer;
    private final Map<String, AllyRequest> requests = new ConcurrentHashMap<>();

    public RelationService(Supplier<Settings> settings, Messages messages, TeamService teams, DataWriter writer) {
        this.settings = settings;
        this.messages = messages;
        this.teams = teams;
        this.writer = writer;
    }

    /** Asks the other team to become allies, or accepts when they already asked. */
    public void ally(Player player, String otherName) {
        Settings s = settings.get();
        if (!s.alliesEnabled) {
            messages.send(player, "error.feature-disabled");
            return;
        }
        Team mine = teams.requireTeam(player);
        if (mine == null || !teams.requirePermission(player, mine, TeamPermission.RELATIONS)) {
            return;
        }
        Team other = target(player, mine, otherName);
        if (other == null) {
            return;
        }
        if (mine.alliedWith(other.id())) {
            messages.send(player, "relations.already-allies", teams.format().team(other));
            return;
        }
        if (s.maxAllies > 0 && mine.allies().size() >= s.maxAllies) {
            messages.send(player, "relations.max-allies", "max", String.valueOf(s.maxAllies));
            return;
        }
        long now = System.currentTimeMillis();
        AllyRequest theirs = requests.get(key(other.id(), mine.id()));
        if (theirs != null && !theirs.expired(now)) {
            if (s.maxAllies > 0 && other.allies().size() >= s.maxAllies) {
                messages.send(player, "relations.other-max-allies", teams.format().team(other, "max", String.valueOf(s.maxAllies)));
                return;
            }
            setRelation(player, mine, other, Relation.ALLY);
            return;
        }
        AllyRequest existing = requests.get(key(mine.id(), other.id()));
        if (existing != null && !existing.expired(now)) {
            messages.send(player, "relations.request-pending", teams.format().team(other));
            return;
        }
        requests.put(key(mine.id(), other.id()), new AllyRequest(mine.id(), other.id(), now + s.allyRequestExpireSeconds * 1000L));
        teams.broadcast(mine, "relations.request-sent", teams.format().team(other, "player", teams.display(player)));
        teams.broadcastTo(other, TeamPermission.RELATIONS, "relations.request-received", teams.format().team(mine,
                "accept", messages.button("accept-ally", "/team ally " + mine.name(), "team", mine.name())));
    }

    public void enemy(Player player, String otherName) {
        if (!settings.get().enemiesEnabled) {
            messages.send(player, "error.feature-disabled");
            return;
        }
        Team mine = teams.requireTeam(player);
        if (mine == null || !teams.requirePermission(player, mine, TeamPermission.RELATIONS)) {
            return;
        }
        Team other = target(player, mine, otherName);
        if (other == null) {
            return;
        }
        if (mine.enemyOf(other.id())) {
            messages.send(player, "relations.already-enemies", teams.format().team(other));
            return;
        }
        setRelation(player, mine, other, Relation.ENEMY);
    }

    public void neutral(Player player, String otherName) {
        Team mine = teams.requireTeam(player);
        if (mine == null || !teams.requirePermission(player, mine, TeamPermission.RELATIONS)) {
            return;
        }
        Team other = target(player, mine, otherName);
        if (other == null) {
            return;
        }
        requests.remove(key(mine.id(), other.id()));
        if (!mine.alliedWith(other.id()) && !mine.enemyOf(other.id())) {
            messages.send(player, "relations.already-neutral", teams.format().team(other));
            return;
        }
        setRelation(player, mine, other, Relation.NEUTRAL);
    }

    /** Changes how two teams relate. Alliances are mutual; an enemy declaration is one team's own. */
    public boolean setRelation(CommandSender actor, Team mine, Team other, Relation to) {
        Relation from = mine.alliedWith(other.id()) ? Relation.ALLY : mine.enemyOf(other.id()) ? Relation.ENEMY : Relation.NEUTRAL;
        if (!TeamService.call(new TeamRelationChangeEvent(mine.id(), mine.name(), other.id(), other.name(), from, to))) {
            messages.send(actor, "error.cancelled");
            return false;
        }
        requests.remove(key(mine.id(), other.id()));
        requests.remove(key(other.id(), mine.id()));
        boolean wasAlly = from == Relation.ALLY;
        switch (to) {
            case ALLY -> {
                mine.addAlly(other.id());
                other.addAlly(mine.id());
                writer.write("alliance", repository -> {
                    repository.saveRelation(mine.id(), other.id(), "ALLY");
                    repository.saveRelation(other.id(), mine.id(), "ALLY");
                });
                teams.broadcast(mine, "relations.allied", teams.format().team(other));
                teams.broadcast(other, "relations.allied", teams.format().team(mine));
            }
            case ENEMY -> {
                mine.addEnemy(other.id());
                if (wasAlly) {
                    other.clearRelation(mine.id());
                }
                writer.write("enemy", repository -> {
                    repository.saveRelation(mine.id(), other.id(), "ENEMY");
                    if (wasAlly) {
                        repository.saveRelation(other.id(), mine.id(), null);
                    }
                });
                teams.broadcast(mine, "relations.enemy-declared", teams.format().team(other));
                teams.broadcast(other, "relations.enemy-received", teams.format().team(mine));
            }
            default -> {
                mine.clearRelation(other.id());
                if (wasAlly) {
                    other.clearRelation(mine.id());
                }
                writer.write("neutral", repository -> {
                    repository.saveRelation(mine.id(), other.id(), null);
                    if (wasAlly) {
                        repository.saveRelation(other.id(), mine.id(), null);
                    }
                });
                teams.broadcast(mine, wasAlly ? "relations.alliance-ended" : "relations.neutral", teams.format().team(other));
                if (wasAlly) {
                    teams.broadcast(other, "relations.alliance-ended", teams.format().team(mine));
                }
            }
        }
        return true;
    }

    /** Ally requests other teams have sent to this team, still pending. */
    public List<Team> incoming(UUID teamId) {
        long now = System.currentTimeMillis();
        requests.values().removeIf(request -> request.expired(now));
        List<Team> out = new ArrayList<>();
        for (AllyRequest request : requests.values()) {
            if (request.toTeam().equals(teamId)) {
                Team from = teams.teamById(request.fromTeam());
                if (from != null) {
                    out.add(from);
                }
            }
        }
        return out;
    }

    public void forget(UUID teamId) {
        requests.values().removeIf(request -> request.fromTeam().equals(teamId) || request.toTeam().equals(teamId));
    }

    private Team target(Player player, Team mine, String otherName) {
        Team other = teams.teamByName(otherName);
        if (other == null) {
            messages.send(player, "error.team-not-found", "team", otherName);
            return null;
        }
        if (other.id().equals(mine.id())) {
            messages.send(player, "relations.self");
            return null;
        }
        return other;
    }

    private static String key(UUID from, UUID to) {
        return from + ">" + to;
    }
}
