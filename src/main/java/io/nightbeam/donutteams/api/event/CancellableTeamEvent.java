package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/** Base of Donut Teams' cancellable events: the team they are about and a cancelled flag. */
public abstract class CancellableTeamEvent extends Event implements Cancellable {

    private final UUID teamId;
    private final String teamName;
    private boolean cancelled;

    protected CancellableTeamEvent(UUID teamId, String teamName) {
        this.teamId = teamId;
        this.teamName = teamName;
    }

    /** The team, or null for {@link TeamCreateEvent} (the team does not exist yet). */
    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
