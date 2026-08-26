package io.nightbeam.donutteams.scheduler;

import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Folia-first scheduler. Uses Paper regional APIs on both Paper and Folia.
 * Never calls {@code BukkitScheduler}.
 */
public final class FoliaScheduler {

    private final Plugin plugin;
    private final boolean folia;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, scheduled -> safe(task));
    }

    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> safe(task));
    }

    public void runGlobalDelayed(Runnable task, long delayTicks) {
        long delay = Math.max(1L, delayTicks);
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduled -> safe(task), delay);
    }

    public void runAtLocation(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Bukkit.getRegionScheduler().execute(plugin, location, () -> safe(task));
    }

    public void runAtRegion(World world, int chunkX, int chunkZ, Runnable task) {
        Objects.requireNonNull(world, "world");
        Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, () -> safe(task));
    }

    public void runForEntity(Entity entity, Runnable task) {
        if (entity == null) {
            runGlobal(task);
            return;
        }
        entity.getScheduler().run(plugin, scheduled -> safe(task), null);
    }

    public void runLaterForEntity(Entity entity, Runnable task, long delayTicks) {
        if (entity == null) {
            runGlobalDelayed(task, delayTicks);
            return;
        }
        long delay = Math.max(1L, delayTicks);
        entity.getScheduler().runDelayed(plugin, scheduled -> safe(task), null, delay);
    }

    public void teleport(Player player, Location location) {
        player.teleportAsync(location);
    }

    private void safe(Runnable task) {
        try {
            task.run();
        } catch (Throwable thrown) {
            plugin.getLogger().log(Level.SEVERE, "Scheduled task failed", thrown);
        }
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            String server = Bukkit.getServer().getClass().getName().toLowerCase();
            return server.contains("folia") || server.contains("regionized");
        }
    }
}
