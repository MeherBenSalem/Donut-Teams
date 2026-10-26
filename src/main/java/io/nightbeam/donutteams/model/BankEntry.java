package io.nightbeam.donutteams.model;

import java.util.UUID;

/** One line of a team's bank history. */
public record BankEntry(UUID teamId, UUID playerId, String playerName, Type type, double amount, double balanceAfter, long atMillis) {

    public enum Type {
        DEPOSIT,
        WITHDRAW,
        LEVEL_UP,
        ADMIN
    }
}
