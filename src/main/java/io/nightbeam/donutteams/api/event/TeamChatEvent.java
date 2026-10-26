package io.nightbeam.donutteams.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A player is about to send a team or ally chat message. Cancel to stop it, or change the message. */
public final class TeamChatEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean allyChat;
    private String message;

    public TeamChatEvent(Player player, UUID teamId, String teamName, String message, boolean allyChat) {
        super(teamId, teamName);
        this.player = player;
        this.message = message;
        this.allyChat = allyChat;
    }

    public Player getPlayer() {
        return player;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public boolean isAllyChat() {
        return allyChat;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
