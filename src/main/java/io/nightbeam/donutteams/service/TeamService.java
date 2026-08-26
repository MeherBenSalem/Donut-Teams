package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.api.event.TeamCreateEvent;
import io.nightbeam.donutteams.api.event.TeamJoinEvent;
import io.nightbeam.donutteams.api.event.TeamLeaveEvent;
import io.nightbeam.donutteams.api.TeamSnapshot;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.PluginSettings;
import io.nightbeam.donutteams.hook.DonutCoreHook;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamHome;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.model.TeamSettings;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.storage.SqlTeamRepository;
import io.nightbeam.donutteams.util.TeamNameValidator;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class TeamService {

    private final DonutTeamsPlugin plugin;
    private final PluginSettings settings;
    private final Messages messages;
    private final FoliaScheduler scheduler;
    private final SqlTeamRepository repository;
    private final TeamCache cache;
    private final SlotService slots;
    private final DonutCoreHook donutCore;

    public TeamService(
            DonutTeamsPlugin plugin,
            PluginSettings settings,
            Messages messages,
            FoliaScheduler scheduler,
            SqlTeamRepository repository,
            TeamCache cache,
            SlotService slots,
            DonutCoreHook donutCore
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.scheduler = scheduler;
        this.repository = repository;
        this.cache = cache;
        this.slots = slots;
        this.donutCore = donutCore;
    }

    public void load() throws SQLException {
        cache.replaceAll(repository.loadAll(), repository.loadInvites());
        repository.deleteExpiredInvites(System.currentTimeMillis());
    }

    public TeamCache cache() {
        return cache;
    }

    public Team teamByPlayer(UUID playerId) {
        return cache.byPlayer(playerId);
    }

    public Team teamById(UUID teamId) {
        return cache.byId(teamId);
    }

    public Team teamByName(String name) {
        return cache.byName(name);
    }

    public Optional<TeamSnapshot> snapshot(UUID playerId) {
        return Optional.ofNullable(toSnapshot(cache.byPlayer(playerId)));
    }

    public Optional<TeamSnapshot> snapshotById(UUID teamId) {
        return Optional.ofNullable(toSnapshot(cache.byId(teamId)));
    }

    public Optional<TeamSnapshot> snapshotByName(String name) {
        return Optional.ofNullable(toSnapshot(cache.byName(name)));
    }

    public TeamSnapshot toSnapshot(Team team) {
        if (team == null) {
            return null;
        }
        TeamHome home = team.home();
        return new TeamSnapshot(
                team.id(),
                team.name(),
                team.tag(),
                team.ownerId(),
                team.size(),
                team.friendlyFire(),
                home == null ? null : home.world());
    }

    public boolean teammates(UUID first, UUID second) {
        if (first == null || second == null || first.equals(second)) {
            return false;
        }
        Team left = cache.byPlayer(first);
        Team right = cache.byPlayer(second);
        return left != null && right != null && left.id().equals(right.id());
    }

    public void create(Player player, String rawName, String rawTag) {
        if (!player.hasPermission("donutteams.create")) {
            messages.send(player, "create.no-permission", "<red>You cannot create a team.");
            return;
        }
        if (cache.byPlayer(player.getUniqueId()) != null) {
            messages.send(player, "already-in-team", "<red>You are already in a team.");
            return;
        }
        String name = TeamNameValidator.normalizeName(rawName);
        String tag = TeamNameValidator.normalizeTag(rawTag == null || rawTag.isBlank() ? name : rawTag);
        if (tag.length() > settings.tagMax()) {
            tag = tag.substring(0, settings.tagMax());
        }
        if (!TeamNameValidator.validName(name, settings.nameMin(), settings.nameMax())) {
            messages.send(player, "create.invalid-name", "<red>Invalid name.",
                    "min", String.valueOf(settings.nameMin()), "max", String.valueOf(settings.nameMax()));
            return;
        }
        if (!TeamNameValidator.validTag(tag, settings.tagMin(), settings.tagMax())) {
            messages.send(player, "create.invalid-tag", "<red>Invalid tag.",
                    "min", String.valueOf(settings.tagMin()), "max", String.valueOf(settings.tagMax()));
            return;
        }
        if (cache.byName(name) != null) {
            messages.send(player, "create.exists", "<red>Exists.", "team", name);
            return;
        }
        TeamCreateEvent event = new TeamCreateEvent(player, name, tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            messages.send(player, "cancelled", "<red>Action cancelled.");
            return;
        }
        UUID teamId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        Team team = new Team(teamId, name, tag, player.getUniqueId(), now);
        TeamMember owner = new TeamMember(player.getUniqueId(), display(player), TeamRole.OWNER, TeamPermission.all(), now);
        TeamSettings teamSettings = TeamSettings.defaults(teamId, settings.defaultFriendlyFire());
        team.putMember(owner);
        team.settings(teamSettings);
        String finalTag = tag;
        async(player, () -> {
            repository.insertTeam(team, owner, teamSettings);
            cache.put(team);
        }, () -> messages.send(player, "create.created", "<green>Created.", "team", name, "tag", finalTag));
    }

    public void disband(Player player, boolean confirmed) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "owner-only", "<red>Only the team owner can do that.");
            return;
        }
        if (!confirmed) {
            messages.send(player, "disband.confirm", "<yellow>Confirm.", "team", team.name());
            return;
        }
        for (TeamMember member : team.members()) {
            Player online = Bukkit.getPlayer(member.playerId());
            TeamLeaveEvent event = new TeamLeaveEvent(online, member.playerId(), team.id(), team.name(), TeamLeaveEvent.Reason.DISBAND);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                messages.send(player, "cancelled", "<red>Action cancelled.");
                return;
            }
        }
        async(player, () -> {
            repository.deleteTeam(team.id());
            cache.remove(team);
        }, () -> {
            messages.send(player, "disband.disbanded", "<red>Disbanded.", "team", team.name());
            broadcast(team, "disband.disbanded", "<red>Disbanded.", "team", team.name());
        });
    }

    public void invite(Player player, Player target) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null || !member.has(TeamPermission.INVITE)) {
            messages.send(player, "no-permission-team", "<red>Your team role cannot do that.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messages.send(player, "invite.self", "<red>You cannot invite yourself.");
            return;
        }
        if (cache.byPlayer(target.getUniqueId()) != null || cache.invite(team.id(), target.getUniqueId()) != null) {
            messages.send(player, "invite.already", "<red>Already invited.");
            return;
        }
        int max = slots.maxMembers(team.ownerId());
        if (team.size() >= max) {
            messages.send(player, "invite.full", "<red>Full.", "max", String.valueOf(max));
            return;
        }
        TeamInvite invite = new TeamInvite(
                team.id(),
                target.getUniqueId(),
                player.getUniqueId(),
                System.currentTimeMillis() + settings.inviteExpireSeconds() * 1000L);
        async(player, () -> {
            repository.insertInvite(invite);
            cache.putInvite(invite);
        }, () -> {
            messages.send(player, "invite.sent", "<green>Invited.", "player", display(target), "team", team.name());
            messages.send(target, "invite.received", "<green>Invited you.", "player", display(player), "team", team.name());
        });
    }

    public void join(Player player, String teamName) {
        if (cache.byPlayer(player.getUniqueId()) != null) {
            messages.send(player, "already-in-team", "<red>You are already in a team.");
            return;
        }
        Team team = cache.byName(teamName);
        if (team == null) {
            messages.send(player, "team-not-found", "<red>Not found.", "team", teamName);
            return;
        }
        TeamInvite invite = cache.invite(team.id(), player.getUniqueId());
        if (invite == null) {
            messages.send(player, "join.no-invite", "<red>No invite.", "team", team.name());
            return;
        }
        if (invite.expired()) {
            cache.removeInvite(team.id(), player.getUniqueId());
            messages.send(player, "join.expired", "<red>Expired.");
            return;
        }
        int max = slots.maxMembers(team.ownerId());
        if (team.size() >= max) {
            messages.send(player, "join.full", "<red>Full.");
            return;
        }
        TeamJoinEvent event = new TeamJoinEvent(player, team.id(), team.name());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            messages.send(player, "cancelled", "<red>Action cancelled.");
            return;
        }
        TeamMember joining = new TeamMember(
                player.getUniqueId(),
                display(player),
                TeamRole.MEMBER,
                TeamPermission.defaultMember(),
                System.currentTimeMillis());
        async(player, () -> {
            synchronized (lock(team.id())) {
                if (team.size() >= slots.maxMembers(team.ownerId())) {
                    throw new IllegalStateException("full");
                }
                repository.upsertMember(team.id(), joining);
                repository.deleteInvite(team.id(), player.getUniqueId());
                team.putMember(joining);
                cache.indexPlayer(player.getUniqueId(), team.id());
                cache.removeInvite(team.id(), player.getUniqueId());
            }
        }, () -> {
            messages.send(player, "join.joined", "<green>Joined.", "player", display(player), "team", team.name());
            broadcast(team, "join.joined", "<green>Joined.", "player", display(player), "team", team.name());
        }, error -> {
            if (error instanceof IllegalStateException) {
                messages.send(player, "join.full", "<red>Full.");
            } else {
                fail(player, error);
            }
        });
    }

    public void leave(Player player) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (team.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "leave.owner", "<red>Transfer or disband first.");
            return;
        }
        TeamLeaveEvent event = new TeamLeaveEvent(player, player.getUniqueId(), team.id(), team.name(), TeamLeaveEvent.Reason.LEAVE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            messages.send(player, "cancelled", "<red>Action cancelled.");
            return;
        }
        async(player, () -> {
            repository.deleteMember(player.getUniqueId());
            team.removeMember(player.getUniqueId());
            cache.unindexPlayer(player.getUniqueId());
        }, () -> {
            messages.send(player, "leave.left", "<yellow>Left.", "team", team.name());
            broadcast(team, "leave.broadcast", "<yellow>Left.", "player", display(player), "team", team.name());
        });
    }

    public void kick(Player player, String targetName) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        TeamMember actor = team.member(player.getUniqueId());
        if (actor == null || !actor.has(TeamPermission.KICK)) {
            messages.send(player, "no-permission-team", "<red>Your team role cannot do that.");
            return;
        }
        TeamMember target = findMember(team, targetName);
        if (target == null) {
            messages.send(player, "kick.not-member", "<red>Not a member.");
            return;
        }
        if (target.isOwner()) {
            messages.send(player, "kick.cannot-owner", "<red>Cannot kick owner.");
            return;
        }
        Player online = Bukkit.getPlayer(target.playerId());
        TeamLeaveEvent event = new TeamLeaveEvent(online, target.playerId(), team.id(), team.name(), TeamLeaveEvent.Reason.KICK);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            messages.send(player, "cancelled", "<red>Action cancelled.");
            return;
        }
        async(player, () -> {
            repository.deleteMember(target.playerId());
            team.removeMember(target.playerId());
            cache.unindexPlayer(target.playerId());
        }, () -> {
            messages.send(player, "kick.kicked", "<red>Kicked.", "player", target.name(), "team", team.name());
            if (online != null) {
                messages.send(online, "kick.target", "<red>Kicked.", "team", team.name());
            }
            broadcast(team, "kick.kicked", "<red>Kicked.", "player", target.name(), "team", team.name());
        });
    }

    public void info(Player player, String teamName) {
        Team team = teamName == null || teamName.isBlank() ? cache.byPlayer(player.getUniqueId()) : cache.byName(teamName);
        if (team == null) {
            messages.send(player, teamName == null || teamName.isBlank() ? "not-in-team" : "team-not-found",
                    "<red>Not found.", "team", teamName == null ? "" : teamName);
            return;
        }
        TeamMember owner = team.member(team.ownerId());
        TeamHome home = team.home();
        messages.send(player, "info.header", "<gold><team>", "team", team.name(), "tag", team.tag());
        messages.send(player, "info.owner", "<gray>Owner", "owner", owner == null ? "?" : owner.name());
        messages.send(player, "info.members", "<gray>Members",
                "count", String.valueOf(team.size()), "max", String.valueOf(slots.maxMembers(team.ownerId())));
        messages.send(player, "info.pvp", "<gray>PvP", "state", team.friendlyFire() ? "ON" : "OFF");
        messages.send(player, "info.home", "<gray>Home", "home", home == null
                ? messages.raw("info.none", "none")
                : home.world() + " " + (int) home.x() + " " + (int) home.y() + " " + (int) home.z());
    }

    public void setHome(Player player) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null || !member.has(TeamPermission.SETHOME)) {
            messages.send(player, "sethome.no-permission", "<red>No permission.");
            return;
        }
        Location location = player.getLocation();
        TeamHome home = new TeamHome(
                team.id(),
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        async(player, () -> {
            repository.upsertHome(home);
            team.home(home);
        }, () -> messages.send(player, "sethome.set", "<green>Home set."));
    }

    public void deleteHome(Player player) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null || !member.has(TeamPermission.SETHOME)) {
            messages.send(player, "delhome.no-permission", "<red>No permission.");
            return;
        }
        if (team.home() == null) {
            messages.send(player, "delhome.none", "<red>No home.");
            return;
        }
        async(player, () -> {
            repository.deleteHome(team.id());
            team.home(null);
        }, () -> messages.send(player, "delhome.deleted", "<yellow>Deleted."));
    }

    public void togglePvp(Player player) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null || !member.has(TeamPermission.PVP)) {
            messages.send(player, "pvp.no-permission", "<red>No permission.");
            return;
        }
        boolean next = !team.friendlyFire();
        TeamSettings nextSettings = new TeamSettings(team.id(), next);
        async(player, () -> {
            repository.upsertSettings(nextSettings);
            team.settings(nextSettings);
        }, () -> messages.send(player, next ? "pvp.enabled" : "pvp.disabled",
                next ? "<red>ON" : "<green>OFF"));
    }

    public void transfer(Player player, String targetName) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "owner-only", "<red>Only the team owner can do that.");
            return;
        }
        TeamMember target = findMember(team, targetName);
        if (target == null || target.playerId().equals(player.getUniqueId())) {
            messages.send(player, "transfer.not-member", "<red>Not a member.");
            return;
        }
        TeamMember oldOwner = team.member(player.getUniqueId());
        TeamMember newOwner = target.withRole(TeamRole.OWNER, TeamPermission.all());
        TeamMember demoted = oldOwner.withRole(TeamRole.MEMBER, TeamPermission.defaultMember());
        async(player, () -> {
            repository.upsertMember(team.id(), newOwner);
            repository.upsertMember(team.id(), demoted);
            repository.updateOwner(team.id(), newOwner.playerId());
            team.putMember(newOwner);
            team.putMember(demoted);
            team.ownerId(newOwner.playerId());
        }, () -> {
            messages.send(player, "transfer.transferred", "<green>Transferred.", "team", team.name(), "player", newOwner.name());
            broadcast(team, "transfer.transferred", "<green>Transferred.", "team", team.name(), "player", newOwner.name());
        });
    }

    public void togglePermission(Player owner, UUID targetId, TeamPermission permission) {
        Team team = requireTeam(owner);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(owner.getUniqueId())) {
            messages.send(owner, "owner-only", "<red>Only the team owner can do that.");
            return;
        }
        TeamMember target = team.member(targetId);
        if (target == null || target.isOwner()) {
            return;
        }
        EnumSet<TeamPermission> next = EnumSet.copyOf(target.permissions());
        if (!next.add(permission)) {
            next.remove(permission);
        }
        TeamMember updated = target.withPermissions(next);
        async(owner, () -> {
            repository.upsertMember(team.id(), updated);
            team.putMember(updated);
        }, () -> plugin.guis().openMember(owner, targetId));
    }

    public void updateName(Player player) {
        Team team = cache.byPlayer(player.getUniqueId());
        if (team == null) {
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null) {
            return;
        }
        String name = display(player);
        if (name.equals(member.name())) {
            return;
        }
        member.name(name);
        scheduler.runAsync(() -> {
            try {
                repository.updateMemberName(player.getUniqueId(), name);
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to update member name", exception);
            }
        });
    }

    public boolean canUseHome(Player player) {
        Team team = cache.byPlayer(player.getUniqueId());
        if (team == null) {
            return false;
        }
        TeamMember member = team.member(player.getUniqueId());
        return member != null && member.has(TeamPermission.HOME) && player.hasPermission("donutteams.home");
    }

    public String display(Player player) {
        return donutCore.resolveDisplayName(player);
    }

    public void broadcast(Team team, String path, String def, String... keyValues) {
        for (TeamMember member : team.members()) {
            Player online = Bukkit.getPlayer(member.playerId());
            if (online != null) {
                Player target = online;
                scheduler.runForEntity(target, () -> messages.send(target, path, def, keyValues));
            }
        }
    }

    public TeamMember findMember(Team team, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return team.member(online.getUniqueId());
        }
        for (TeamMember member : team.members()) {
            if (member.name().equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    private Team requireTeam(Player player) {
        Team team = cache.byPlayer(player.getUniqueId());
        if (team == null) {
            messages.send(player, "not-in-team", "<red>You are not in a team.");
        }
        return team;
    }

    private void async(Player player, CheckedRunnable work, Runnable success) {
        async(player, work, success, error -> fail(player, error));
    }

    private void async(Player player, CheckedRunnable work, Runnable success, Consumer<Exception> failure) {
        scheduler.runAsync(() -> {
            try {
                work.run();
                scheduler.runForEntity(player, success);
            } catch (Exception exception) {
                scheduler.runForEntity(player, () -> failure.accept(exception));
            }
        });
    }

    private void fail(Player player, Exception exception) {
        plugin.getLogger().log(Level.SEVERE, "Team action failed", exception);
        messages.send(player, "error", "<red>Something went wrong.");
    }

    private static Object lock(UUID teamId) {
        return teamId.toString().intern();
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
