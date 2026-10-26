package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A team is about to change its name or tag. Cancel to stop it. */
public final class TeamRenameEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String newName;
    private final String oldTag;
    private final String newTag;

    public TeamRenameEvent(UUID teamId, String oldName, String newName, String oldTag, String newTag) {
        super(teamId, oldName);
        this.newName = newName;
        this.oldTag = oldTag;
        this.newTag = newTag;
    }

    public String getNewName() {
        return newName;
    }

    public String getOldTag() {
        return oldTag;
    }

    public String getNewTag() {
        return newTag;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
