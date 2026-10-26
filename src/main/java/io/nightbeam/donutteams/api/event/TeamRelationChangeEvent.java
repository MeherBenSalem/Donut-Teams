package io.nightbeam.donutteams.api.event;

import io.nightbeam.donutteams.model.Relation;
import java.util.UUID;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Two teams are about to become allies, enemies or neutral. Cancel to stop it. */
public final class TeamRelationChangeEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID otherTeamId;
    private final String otherTeamName;
    private final Relation from;
    private final Relation to;

    public TeamRelationChangeEvent(UUID teamId, String teamName, UUID otherTeamId, String otherTeamName, Relation from, Relation to) {
        super(teamId, teamName);
        this.otherTeamId = otherTeamId;
        this.otherTeamName = otherTeamName;
        this.from = from;
        this.to = to;
    }

    public UUID getOtherTeamId() {
        return otherTeamId;
    }

    public String getOtherTeamName() {
        return otherTeamName;
    }

    public Relation getFrom() {
        return from;
    }

    public Relation getTo() {
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
