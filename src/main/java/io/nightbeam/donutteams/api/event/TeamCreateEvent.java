package io.nightbeam.donutteams.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class TeamCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String teamName;
    private final String teamTag;
    private boolean cancelled;

    public TeamCreateEvent(Player player, String teamName, String teamTag) {
        this.player = player;
        this.teamName = teamName;
        this.teamTag = teamTag;
    }

    public Player getPlayer() {
        return player;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getTeamTag() {
        return teamTag;
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
