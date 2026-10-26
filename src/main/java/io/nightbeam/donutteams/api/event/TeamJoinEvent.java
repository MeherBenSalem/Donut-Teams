package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A player is about to join a team (invite, open team, accepted request or admin). Cancel to stop it. */
public final class TeamJoinEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public TeamJoinEvent(Player player, UUID teamId, String teamName) {
        super(teamId, teamName);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
