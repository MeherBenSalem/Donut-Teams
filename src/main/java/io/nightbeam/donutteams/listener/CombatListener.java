package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.service.TeamService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Friendly fire between teammates and allies (melee, arrows, tridents, pets, harmful potions), kill and death
 * counting, and cancelling a home warmup on damage. On Folia these events run on the victim's thread; only the
 * thread-safe team cache is read here.
 */
public final class CombatListener implements Listener {

    private static final long NOTICE_MILLIS = 1500L;

    private final DonutTeamsPlugin plugin;
    private final TeamService teams;
    private final Map<UUID, Long> notices = new ConcurrentHashMap<>();

    public CombatListener(DonutTeamsPlugin plugin) {
        this.plugin = plugin;
        this.teams = plugin.teams();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (protectedFrom(attacker, victim)) {
            event.setCancelled(true);
            notice(attacker, victim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        Player thrower = attacker(potion);
        if (thrower == null || !harmful(potion.getEffects())) {
            return;
        }
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player victim && !victim.getUniqueId().equals(thrower.getUniqueId()) && protectedFrom(thrower, victim)) {
                event.setIntensity(victim, 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        ProjectileSource source = cloud.getSource();
        if (!(source instanceof Player thrower)) {
            return;
        }
        if (!harmful(cloud.getCustomEffects()) && !baseHarmful(cloud)) {
            return;
        }
        event.getAffectedEntities().removeIf(entity -> entity instanceof Player victim
                && !victim.getUniqueId().equals(thrower.getUniqueId()) && protectedFrom(thrower, victim));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && plugin.homes().warmingUp(player.getUniqueId())) {
            plugin.homes().onDamage(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        teams.recordKill(victim.getKiller(), victim);
    }

    private boolean protectedFrom(Player attacker, Player victim) {
        Relation relation = teams.relation(attacker.getUniqueId(), victim.getUniqueId());
        if (relation == Relation.TEAMMATE) {
            Team team = teams.teamByPlayer(victim.getUniqueId());
            return team != null && !team.friendlyFire() && !attacker.hasPermission("donutteams.bypass.friendlyfire");
        }
        if (relation == Relation.ALLY) {
            Team mine = teams.teamByPlayer(attacker.getUniqueId());
            Team theirs = teams.teamByPlayer(victim.getUniqueId());
            return mine != null && theirs != null && !mine.allyFriendlyFire() && !theirs.allyFriendlyFire()
                    && !attacker.hasPermission("donutteams.bypass.friendlyfire");
        }
        return false;
    }

    private void notice(Player attacker, Player victim) {
        long now = System.currentTimeMillis();
        Long last = notices.get(attacker.getUniqueId());
        if (last != null && now - last < NOTICE_MILLIS) {
            return;
        }
        notices.put(attacker.getUniqueId(), now);
        boolean teammate = teams.teammates(attacker.getUniqueId(), victim.getUniqueId());
        String name = victim.getName();
        plugin.scheduler().forPlayer(attacker, () ->
                plugin.messages().actionBar(attacker, teammate ? "pvp.blocked" : "pvp.ally-blocked", "player", name));
    }

    /** The cloud's base potion: {@code getBasePotionType} from 1.20.5, {@code getBasePotionData} before. */
    private static boolean baseHarmful(AreaEffectCloud cloud) {
        try {
            Object type = cloud.getClass().getMethod("getBasePotionType").invoke(cloud);
            if (type == null) {
                return false;
            }
            Object effects = type.getClass().getMethod("getPotionEffects").invoke(type);
            return effects instanceof Iterable<?> list && harmful(cast(list));
        } catch (ReflectiveOperationException ignored) {
            // Older API below.
        } catch (Throwable ignored) {
            return false;
        }
        try {
            Object data = cloud.getClass().getMethod("getBasePotionData").invoke(cloud);
            if (data == null) {
                return false;
            }
            Object type = data.getClass().getMethod("getType").invoke(data);
            Object effect = type.getClass().getMethod("getEffectType").invoke(type);
            return effect instanceof PotionEffectType effectType && effectType.getEffectCategory() == PotionEffectType.Category.HARMFUL;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Iterable<PotionEffect> cast(Iterable<?> list) {
        return (Iterable<PotionEffect>) list;
    }

    private static boolean harmful(Iterable<PotionEffect> effects) {
        if (effects == null) {
            return false;
        }
        for (PotionEffect effect : effects) {
            if (effect.getType().getEffectCategory() == PotionEffectType.Category.HARMFUL) {
                return true;
            }
        }
        return false;
    }

    static Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        if (damager instanceof Tameable tameable && tameable.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }
}
