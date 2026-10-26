package io.nightbeam.donutteams.model;

import java.util.UUID;

/** One team asking another to become allies. Kept in memory; requests expire. */
public record AllyRequest(UUID fromTeam, UUID toTeam, long expiresAtMillis) {

    public boolean expired(long now) {
        return now > expiresAtMillis;
    }
}
