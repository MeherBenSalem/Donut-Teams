package io.nightbeam.donutteams.model;

import java.util.UUID;

/** A player asking to join a team that is not open. Kept in memory; requests expire. */
public record JoinRequest(UUID teamId, UUID playerId, String playerName, long expiresAtMillis) {

    public boolean expired(long now) {
        return now > expiresAtMillis;
    }
}
