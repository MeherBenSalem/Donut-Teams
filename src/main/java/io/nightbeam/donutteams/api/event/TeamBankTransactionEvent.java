package io.nightbeam.donutteams.api.event;

import io.nightbeam.donutteams.model.BankEntry;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Money is about to move in or out of a team bank (deposit, withdrawal, level-up). Cancel to stop it. */
public final class TeamBankTransactionEvent extends CancellableTeamEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final BankEntry.Type type;
    private final double amount;

    public TeamBankTransactionEvent(UUID teamId, String teamName, @Nullable Player player, BankEntry.Type type, double amount) {
        super(teamId, teamName);
        this.player = player;
        this.type = type;
        this.amount = amount;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    public BankEntry.Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
