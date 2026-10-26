package io.nightbeam.donutteams.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A player is about to create a team. Cancel to stop it. */
public final class TeamCreateEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String teamTag;

    public TeamCreateEvent(Player player, String teamName, String teamTag) {
        super(null, teamName);
        this.player = player;
        this.teamTag = teamTag;
    }

    public Player getPlayer() {
        return player;
    }

    public String getTeamTag() {
        return teamTag;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
