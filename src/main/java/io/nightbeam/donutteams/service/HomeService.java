package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.api.event.TeamHomeTeleportEvent;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.storage.DataWriter;
import io.nightbeam.donutteams.util.Durations;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Named team homes: setting, deleting, and the teleport warmup (cancelled by moving or taking damage). */
public final class HomeService {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_-]{1,16}$");

    private final DonutTeamsPlugin plugin;
    private final Supplier<Settings> settings;
    private final Messages messages;
    private final FoliaScheduler scheduler;
    private final TeamService teams;
    private final DataWriter writer;
    private final Map<UUID, Warmup> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public HomeService(DonutTeamsPlugin plugin, Supplier<Settings> settings, Messages messages, FoliaScheduler scheduler,
                       TeamService teams, DataWriter writer) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.scheduler = scheduler;
        this.teams = teams;
        this.writer = writer;
    }

    // ── Setting and deleting ────────────────────────────────────────────────────────────────────────────────

    public void setHome(Player player, String rawName) {
        Team team = teams.requireTeam(player);
        if (team == null || !teams.requirePermission(player, team, TeamPermission.SETHOME)) {
            return;
        }
        String name = rawName == null || rawName.isBlank() ? TeamHome.DEFAULT : rawName.trim().toLowerCase(Locale.ROOT);
        if (!NAME.matcher(name).matches()) {
            messages.send(player, "home.invalid-name");
            return;
        }
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null || disabled(world)) {
            messages.send(player, "home.disabled-world");
            return;
        }
        int max = teams.format().maxHomes(team);
        if (team.home(name) == null && team.homes().size() >= max) {
            boolean one = max == 1 && team.homes().size() == 1;
            messages.send(player, one ? "home.limit-one" : "home.limit",
                    teams.format().team(team, "home", one ? team.homeList().get(0).name() : name));
            return;
        }
        TeamHome home = new TeamHome(team.id(), name, world.getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), player.getUniqueId(), System.currentTimeMillis());
        team.putHome(home);
        writer.write("team home", repository -> repository.saveHome(home));
        teams.broadcast(team, "home.set", teams.format().team(team, "home", name, "player", teams.display(player),
                "coords", home.coordinates(), "world", home.world()));
    }

    public void deleteHome(Player player, String rawName) {
        Team team = teams.requireTeam(player);
        if (team == null || !teams.requirePermission(player, team, TeamPermission.SETHOME)) {
            return;
        }
        TeamHome home = rawName == null || rawName.isBlank() ? team.defaultHome() : team.home(rawName.trim());
        if (home == null) {
            messages.send(player, team.homes().isEmpty() ? "home.none" : "home.not-found", "home", rawName == null ? "" : rawName);
            return;
        }
        team.removeHome(home.name());
        writer.write("team home removal", repository -> repository.deleteHome(team.id(), home.name()));
        teams.broadcast(team, "home.deleted", teams.format().team(team, "home", home.name(), "player", teams.display(player)));
    }

    // ── Teleporting ─────────────────────────────────────────────────────────────────────────────────────────

    public boolean canUse(Player player) {
        return player.hasPermission("donutteams.home") && teams.can(player, TeamPermission.HOME);
    }

    public void start(Player player, String rawName) {
        Team team = teams.requireTeam(player);
        if (team == null) {
            return;
        }
        if (!canUse(player)) {
            messages.send(player, "error.no-team-permission", "permission", teams.format().permission(TeamPermission.HOME));
            return;
        }
        TeamHome home = rawName == null || rawName.isBlank() ? team.defaultHome() : team.home(rawName.trim());
        if (home == null) {
            messages.send(player, team.homes().isEmpty() ? "home.none" : "home.not-found", "home", rawName == null ? "" : rawName);
            return;
        }
        World world = Bukkit.getWorld(home.world());
        if (world == null) {
            messages.send(player, "home.invalid-world", "world", home.world());
            return;
        }
        if (disabled(world)) {
            messages.send(player, "home.disabled-world");
            return;
        }
        Settings s = settings.get();
        Long until = cooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (until != null && until > now && !player.hasPermission("donutteams.bypass.cooldown")) {
            messages.send(player, "home.cooldown", "time", Durations.compact(until - now));
            return;
        }
        cancel(player, false);
        int seconds = player.hasPermission("donutteams.bypass.warmup") ? 0 : s.homeWarmupSeconds;
        if (seconds <= 0) {
            finish(player, team, home);
            return;
        }
        Warmup warmup = new Warmup(team.id(), home, seconds, player.getLocation());
        active.put(player.getUniqueId(), warmup);
        messages.send(player, "home.warmup", "seconds", String.valueOf(seconds), "home", home.name());
        tick(player, warmup);
    }

    /** Movement off the starting block cancels (looking around and jumping in place do not). */
    public void onMove(Player player, Location to) {
        if (!settings.get().homeCancelOnMove) {
            return;
        }
        Warmup warmup = active.get(player.getUniqueId());
        if (warmup == null || !warmup.active.get() || to == null) {
            return;
        }
        if (moved(warmup.start, to)) {
            cancelMoved(player);
        }
    }

    private static boolean moved(Location from, Location to) {
        return from.getWorld() != to.getWorld() || from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()
                || Math.abs(from.getY() - to.getY()) >= 1.3;
    }

    private void cancelMoved(Player player) {
        if (cancel(player, false)) {
            messages.send(player, "home.cancelled-move");
            plugin.sounds().play(player, "error");
        }
    }

    public void onDamage(Player player) {
        if (!settings.get().homeCancelOnDamage) {
            return;
        }
        if (cancel(player, false)) {
            messages.send(player, "home.cancelled-damage");
            plugin.sounds().play(player, "error");
        }
    }

    /** Returns true when a warmup was running. */
    public boolean cancel(Player player, boolean notify) {
        Warmup warmup = active.remove(player.getUniqueId());
        if (warmup == null || !warmup.active.compareAndSet(true, false)) {
            return false;
        }
        if (player.isOnline()) {
            player.sendActionBar(Component.empty());
            if (settings.get().homeTitle) {
                player.clearTitle();
            }
        }
        if (notify) {
            messages.send(player, "home.cancelled");
        }
        return true;
    }

    public void clear(UUID playerId) {
        Warmup warmup = active.remove(playerId);
        if (warmup != null) {
            warmup.active.set(false);
        }
    }

    public boolean warmingUp(UUID playerId) {
        Warmup warmup = active.get(playerId);
        return warmup != null && warmup.active.get();
    }

    private void tick(Player player, Warmup warmup) {
        if (!warmup.active.get()) {
            return;
        }
        if (!player.isOnline()) {
            clear(player.getUniqueId());
            return;
        }
        // Each tick also checks the position: Folia fires no event when another plugin teleports the player.
        if (settings.get().homeCancelOnMove && moved(warmup.start, player.getLocation())) {
            cancelMoved(player);
            return;
        }
        int left = warmup.secondsLeft.getAndDecrement();
        if (left <= 0) {
            if (warmup.active.compareAndSet(true, false)) {
                active.remove(player.getUniqueId(), warmup);
                player.sendActionBar(Component.empty());
                Team team = teams.teamById(warmup.teamId);
                if (team == null || !team.isMember(player.getUniqueId())) {
                    return;
                }
                finish(player, team, warmup.home);
            }
            return;
        }
        messages.actionBar(player, "home.actionbar", "seconds", String.valueOf(left), "home", warmup.home.name());
        if (settings.get().homeTitle) {
            messages.title(player, "home.title", "home.subtitle", 30, "seconds", String.valueOf(left), "home", warmup.home.name());
        }
        plugin.sounds().play(player, "home-tick");
        scheduler.runLaterForEntity(player, () -> tick(player, warmup), 20L);
    }

    private void finish(Player player, Team team, TeamHome home) {
        World world = Bukkit.getWorld(home.world());
        if (world == null) {
            messages.send(player, "home.invalid-world", "world", home.world());
            return;
        }
        Location location = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        TeamHomeTeleportEvent event = new TeamHomeTeleportEvent(player, team.id(), team.name(), home.name(), location);
        if (!TeamService.call(event)) {
            return;
        }
        Location destination = event.getDestination();
        Settings s = settings.get();
        if (s.homeCooldownSeconds > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + s.homeCooldownSeconds * 1000L);
        }
        if (s.homeTitle) {
            player.clearTitle();
        }
        if (s.homeParticles) {
            spawnParticles(player.getLocation());
        }
        player.teleportAsync(destination).thenAccept(success -> {
            if (!Boolean.TRUE.equals(success)) {
                return;
            }
            scheduler.runForEntity(player, () -> {
                messages.send(player, "home.teleported", teams.format().team(team, "home", home.name()));
                plugin.sounds().play(player, "home-teleport");
                if (s.homeParticles) {
                    spawnParticles(player.getLocation());
                }
            });
        });
    }

    private static void spawnParticles(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        try {
            world.spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.4);
        } catch (Throwable ignored) {
            // Particles are cosmetic.
        }
    }

    private boolean disabled(World world) {
        return settings.get().disabledHomeWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    private static final class Warmup {
        private final UUID teamId;
        private final TeamHome home;
        private final AtomicInteger secondsLeft;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final Location start;

        private Warmup(UUID teamId, TeamHome home, int seconds, Location start) {
            this.teamId = teamId;
            this.home = home;
            this.secondsLeft = new AtomicInteger(seconds);
            this.start = start;
        }
    }
}
