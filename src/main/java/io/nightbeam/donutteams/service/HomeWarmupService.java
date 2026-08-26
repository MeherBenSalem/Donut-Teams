package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.PluginSettings;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class HomeWarmupService {

    private final PluginSettings settings;
    private final Messages messages;
    private final FoliaScheduler scheduler;
    private final TeamService teams;
    private final Map<UUID, Warmup> active = new ConcurrentHashMap<>();

    public HomeWarmupService(PluginSettings settings, Messages messages, FoliaScheduler scheduler, TeamService teams) {
        this.settings = settings;
        this.messages = messages;
        this.scheduler = scheduler;
        this.teams = teams;
    }

    public void start(Player player) {
        if (!teams.canUseHome(player)) {
            messages.send(player, "home.no-permission", "<red>No permission.");
            return;
        }
        Team team = teams.teamByPlayer(player.getUniqueId());
        if (team == null) {
            messages.send(player, "not-in-team", "<red>Not in a team.");
            return;
        }
        TeamHome home = team.home();
        if (home == null) {
            messages.send(player, "home.none", "<red>No home.");
            return;
        }
        World world = Bukkit.getWorld(home.world());
        if (world == null) {
            messages.send(player, "home.invalid-world", "<red>World not loaded.");
            return;
        }
        cancel(player, false);
        int seconds = Math.max(0, settings.homeWarmupSeconds());
        if (seconds == 0) {
            finish(player, home);
            return;
        }
        Warmup warmup = new Warmup(player.getUniqueId(), home, seconds);
        active.put(player.getUniqueId(), warmup);
        messages.send(player, "home.warmup", "<yellow>Warmup.", "seconds", String.valueOf(seconds));
        tick(player, warmup);
    }

    public void onMove(Player player) {
        if (!settings.cancelHomeOnMove()) {
            return;
        }
        Warmup warmup = active.get(player.getUniqueId());
        if (warmup == null || !warmup.active.get()) {
            return;
        }
        cancel(player, true);
        messages.send(player, "home.cancelled-move", "<red>Moved.");
    }

    public void cancel(Player player, boolean notify) {
        Warmup warmup = active.remove(player.getUniqueId());
        if (warmup == null || !warmup.active.compareAndSet(true, false)) {
            return;
        }
        if (player.isOnline()) {
            player.sendActionBar(Component.empty());
        }
        if (notify) {
            messages.send(player, "home.cancelled", "<red>Cancelled.");
        }
    }

    public void clear(UUID playerId) {
        Warmup warmup = active.remove(playerId);
        if (warmup != null) {
            warmup.active.set(false);
        }
    }

    private void tick(Player player, Warmup warmup) {
        if (!warmup.active.get()) {
            return;
        }
        if (!player.isOnline()) {
            clear(player.getUniqueId());
            return;
        }
        int left = warmup.secondsLeft.getAndDecrement();
        if (left <= 0) {
            if (warmup.active.compareAndSet(true, false)) {
                active.remove(player.getUniqueId());
                player.sendActionBar(Component.empty());
                finish(player, warmup.home);
            }
            return;
        }
        player.sendActionBar(messages.component("home.actionbar", "<gold>Team home in <white><seconds></white>s",
                "seconds", String.valueOf(left)));
        scheduler.runLaterForEntity(player, () -> tick(player, warmup), 20L);
    }

    private void finish(Player player, TeamHome home) {
        World world = Bukkit.getWorld(home.world());
        if (world == null) {
            messages.send(player, "home.invalid-world", "<red>World not loaded.");
            return;
        }
        Location location = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        scheduler.teleport(player, location);
        messages.send(player, "home.teleported", "<green>Teleported.");
    }

    private static final class Warmup {
        private final UUID playerId;
        private final TeamHome home;
        private final AtomicInteger secondsLeft;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Warmup(UUID playerId, TeamHome home, int seconds) {
            this.playerId = playerId;
            this.home = home;
            this.secondsLeft = new AtomicInteger(seconds);
        }
    }
}
