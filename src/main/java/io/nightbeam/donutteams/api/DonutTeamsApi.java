package io.nightbeam.donutteams.api;

import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.TeamRole;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Donut Teams for other plugins. Get it with {@link DonutTeams#getApi()} or Bukkit's services manager. Every method
 * is safe to call from any thread; results are snapshots.
 */
public interface DonutTeamsApi {

    Optional<TeamSnapshot> teamByPlayer(UUID playerId);

    Optional<TeamSnapshot> teamById(UUID teamId);

    Optional<TeamSnapshot> teamByName(String name);

    boolean teammates(UUID first, UUID second);

    boolean friendlyFire(UUID teamId);

    /** How the first player's team sees the second player: TEAMMATE, ALLY, ENEMY or NEUTRAL. */
    default Relation relation(UUID viewer, UUID target) {
        return Relation.NEUTRAL;
    }

    /** The player's rank, if they are in a team. */
    default Optional<TeamRole> role(UUID playerId) {
        return Optional.empty();
    }

    /** Every team. */
    default Collection<TeamSnapshot> teams() {
        return java.util.List.of();
    }
}
