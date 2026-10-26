package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A member is about to leave a team. Cancel to stop it. */
public final class TeamLeaveEvent extends CancellableTeamEvent {

    public enum Reason {
        LEAVE,
        KICK,
        DISBAND,
        ADMIN
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID playerId;
    private final Reason reason;

    public TeamLeaveEvent(@Nullable Player player, UUID playerId, UUID teamId, String teamName, Reason reason) {
        super(teamId, teamName);
        this.player = player;
        this.playerId = playerId;
        this.reason = reason;
    }

    /** The member, when online. */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
