package io.nightbeam.donutteams.api;

import java.util.Optional;
import java.util.UUID;

public interface DonutTeamsApi {

    Optional<TeamSnapshot> teamByPlayer(UUID playerId);

    Optional<TeamSnapshot> teamById(UUID teamId);

    Optional<TeamSnapshot> teamByName(String name);

    boolean teammates(UUID first, UUID second);

    boolean friendlyFire(UUID teamId);
}
