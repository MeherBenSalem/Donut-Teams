package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TeamLeaveEvent extends Event implements Cancellable {

    public enum Reason {
        LEAVE,
        KICK,
        DISBAND
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID playerId;
    private final UUID teamId;
    private final String teamName;
    private final Reason reason;
    private boolean cancelled;

    public TeamLeaveEvent(@Nullable Player player, UUID playerId, UUID teamId, String teamName, Reason reason) {
        this.player = player;
        this.playerId = playerId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.reason = reason;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
