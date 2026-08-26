package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.service.HomeWarmupService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerMoveListener implements Listener {

    private final HomeWarmupService homes;

    public PlayerMoveListener(HomeWarmupService homes) {
        this.homes = homes;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        homes.onMove(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        homes.onMove(event.getPlayer());
    }
}
