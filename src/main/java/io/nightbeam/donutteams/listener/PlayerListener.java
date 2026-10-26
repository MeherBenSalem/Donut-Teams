package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Joining and leaving the server, and movement during a home warmup. */
public final class PlayerListener implements Listener {

    private final DonutTeamsPlugin plugin;

    public PlayerListener(DonutTeamsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.scheduler().runLaterForEntity(player, () -> {
            plugin.teams().onJoin(player);
            plugin.updates().notify(player);
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.chat().clear(player.getUniqueId());
        plugin.homes().clear(player.getUniqueId());
        plugin.teams().onQuit(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (plugin.homes().warmingUp(event.getPlayer().getUniqueId())) {
            plugin.homes().onMove(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.homes().warmingUp(event.getPlayer().getUniqueId())) {
            plugin.homes().onMove(event.getPlayer(), event.getTo());
        }
    }
}
