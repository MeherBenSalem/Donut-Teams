package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A player's team home warmup has finished and they are about to teleport. Cancel to stop it. */
public final class TeamHomeTeleportEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String homeName;
    private Location destination;

    public TeamHomeTeleportEvent(Player player, UUID teamId, String teamName, String homeName, Location destination) {
        super(teamId, teamName);
        this.player = player;
        this.homeName = homeName;
        this.destination = destination;
    }

    public Player getPlayer() {
        return player;
    }

    public String getHomeName() {
        return homeName;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        if (destination != null && destination.getWorld() != null) {
            this.destination = destination;
        }
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
