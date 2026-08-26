package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.service.TeamService;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Friendly-fire. On Folia this event already runs on the victim's region thread.
 */
public final class FriendlyFireListener implements Listener {

    private final TeamService teams;

    public FriendlyFireListener(TeamService teams) {
        this.teams = teams;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event);
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        // UUID cache lookup is thread-safe; do not mutate attacker entity state here.
        if (!teams.teammates(attacker.getUniqueId(), victim.getUniqueId())) {
            return;
        }
        Team team = teams.teamByPlayer(victim.getUniqueId());
        if (team == null || team.friendlyFire()) {
            return;
        }
        event.setCancelled(true);
    }

    private static Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Tameable tameable && tameable.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }
}
