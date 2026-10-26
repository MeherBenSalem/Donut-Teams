package io.nightbeam.donutteams.api.event;

import io.nightbeam.donutteams.model.TeamRole;
import java.util.UUID;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A member is about to be promoted, demoted, or made owner. Cancel to stop it. */
public final class TeamRoleChangeEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final TeamRole from;
    private final TeamRole to;

    public TeamRoleChangeEvent(UUID teamId, String teamName, UUID playerId, TeamRole from, TeamRole to) {
        super(teamId, teamName);
        this.playerId = playerId;
        this.from = from;
        this.to = to;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public TeamRole getFrom() {
        return from;
    }

    public TeamRole getTo() {
        return to;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
