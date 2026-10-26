package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A team is about to reach a new level (bought from its bank, or set by an admin). Cancel to stop it. */
public final class TeamLevelUpEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int from;
    private final int to;
    private final double cost;

    public TeamLevelUpEvent(UUID teamId, String teamName, int from, int to, double cost) {
        super(teamId, teamName);
        this.from = from;
        this.to = to;
        this.cost = cost;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public double getCost() {
        return cost;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
