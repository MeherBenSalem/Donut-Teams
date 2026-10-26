package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A team is about to be disbanded by its owner or an admin. Cancel to stop it. */
public final class TeamDisbandEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CommandSender actor;
    private final boolean admin;

    public TeamDisbandEvent(CommandSender actor, UUID teamId, String teamName, boolean admin) {
        super(teamId, teamName);
        this.actor = actor;
        this.admin = admin;
    }

    public CommandSender getActor() {
        return actor;
    }

    /** True when an admin disbanded the team with /donutteams disband. */
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
