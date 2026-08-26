package io.nightbeam.donutteams.model;

import java.util.UUID;

public record TeamInvite(UUID teamId, UUID playerId, UUID invitedBy, long expiresAtMillis) {

    public boolean expired() {
        return System.currentTimeMillis() > expiresAtMillis;
    }
}
