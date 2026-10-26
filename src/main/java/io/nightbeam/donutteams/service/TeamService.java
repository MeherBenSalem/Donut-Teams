package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.api.TeamSnapshot;
import io.nightbeam.donutteams.api.event.TeamCreateEvent;
import io.nightbeam.donutteams.api.event.TeamDisbandEvent;
import io.nightbeam.donutteams.api.event.TeamJoinEvent;
import io.nightbeam.donutteams.api.event.TeamLeaveEvent;
import io.nightbeam.donutteams.api.event.TeamRenameEvent;
import io.nightbeam.donutteams.api.event.TeamRoleChangeEvent;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.hook.DonutCoreHook;
import io.nightbeam.donutteams.hook.Economy;
import io.nightbeam.donutteams.model.JoinRequest;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamColor;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.storage.DataWriter;
import io.nightbeam.donutteams.util.ItemCodec;
import io.nightbeam.donutteams.util.SlotPermissionParser;
import io.nightbeam.donutteams.util.TeamNameValidator;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 * Teams and their members. Every change is checked and made in memory under one lock, on the thread of the player
 * (or console) asking for it, then saved in order on the database thread. Messages to other players go through
 * their own thread, so all of this is Folia-safe.
 */
public final class TeamService {

    private final DonutTeamsPlugin plugin;
    private final Supplier<Settings> settings;
    private final Messages messages;
    private final Format format;
    private final FoliaScheduler scheduler;
    private final DataWriter writer;
    private final TeamCache cache;
    private final DonutCoreHook names;
    private final Economy economy;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, JoinRequest> requests = new ConcurrentHashMap<>();
    private volatile ChestService chests;

    public TeamService(DonutTeamsPlugin plugin, Supplier<Settings> settings, Messages messages, Format format, FoliaScheduler scheduler,
                       DataWriter writer, TeamCache cache, DonutCoreHook names, Economy economy) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.format = format;
        this.scheduler = scheduler;
        this.writer = writer;
        this.cache = cache;
        this.names = names;
        this.economy = economy;
    }

    public void chests(ChestService chests) {
        this.chests = chests;
    }

    // ── Loading ─────────────────────────────────────────────────────────────────────────────────────────────

    /** Loads every team (blocking, at start-up). Members upgraded from 1.x get their rank's new 2.0 permissions once. */
    public void load() throws Exception {
        var repository = writer.repository();
        List<Team> teams = repository.loadAll();
        cache.replaceAll(teams, repository.loadInvites());
        repository.deleteExpiredInvites(System.currentTimeMillis());
        if ("1".equals(repository.meta("upgraded_from")) && repository.meta("v1_permissions_granted") == null) {
            int upgraded = 0;
            long now = System.currentTimeMillis();
            for (Team team : teams) {
                for (TeamMember member : team.members()) {
                    // 1.x did not record when members were last online; count from the upgrade.
                    member.lastSeenMillis(now);
                    if (member.isOwner()) {
                        repository.saveMember(team.id(), member);
                        continue;
                    }
                    EnumSet<TeamPermission> next = EnumSet.noneOf(TeamPermission.class);
                    next.addAll(member.permissions());
                    for (TeamPermission permission : settings.get().defaultPermissions(member.role())) {
                        if (TeamPermission.ADDED_IN_2_0.contains(permission)) {
                            next.add(permission);
                        }
                    }
                    member.permissions(next);
                    repository.saveMember(team.id(), member);
                    upgraded++;
                }
            }
            repository.meta("v1_permissions_granted", "1");
            plugin.getLogger().info("Gave " + upgraded + " members from 1.x the 2.0 permissions of their rank.");
        }
    }

    // ── Lookups ─────────────────────────────────────────────────────────────────────────────────────────────

    public TeamCache cache() {
        return cache;
    }

    public Format format() {
        return format;
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
        var home = team.defaultHome();
        return new TeamSnapshot(team.id(), team.name(), team.tag(), team.ownerId(), team.size(), team.friendlyFire(),
                home == null ? null : home.world(), team.color().tag(), team.level(), team.balance(), team.kills(), team.deaths(),
                team.open(), Set.copyOf(team.allies()), Set.copyOf(team.enemies()),
                team.members().stream().map(TeamMember::playerId).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    public boolean teammates(UUID first, UUID second) {
        if (first == null || second == null || first.equals(second)) {
            return false;
        }
        Team left = cache.byPlayer(first);
        return left != null && left.isMember(second);
    }

    /** How the first player's team sees the second player (NEUTRAL when either has no team). */
    public Relation relation(UUID viewer, UUID target) {
        Team mine = cache.byPlayer(viewer);
        Team theirs = cache.byPlayer(target);
        return relation(mine, theirs);
    }

    public Relation relation(Team mine, Team theirs) {
        if (mine == null || theirs == null) {
            return Relation.NEUTRAL;
        }
        if (mine.id().equals(theirs.id())) {
            return Relation.TEAMMATE;
        }
        if (mine.alliedWith(theirs.id())) {
            return Relation.ALLY;
        }
        if (mine.enemyOf(theirs.id()) || theirs.enemyOf(mine.id())) {
            return Relation.ENEMY;
        }
        return Relation.NEUTRAL;
    }

    public String display(Player player) {
        return names.resolveDisplayName(player);
    }

    public TeamMember findMember(Team team, String name) {
        if (team == null || name == null) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && team.isMember(online.getUniqueId())) {
            return team.member(online.getUniqueId());
        }
        for (TeamMember member : team.members()) {
            if (member.name().equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    public boolean can(Player player, TeamPermission permission) {
        Team team = cache.byPlayer(player.getUniqueId());
        TeamMember member = team == null ? null : team.member(player.getUniqueId());
        return member != null && member.has(permission);
    }

    // ── Create / disband ────────────────────────────────────────────────────────────────────────────────────

    public void create(Player player, String rawName, String rawTag) {
        Settings s = settings.get();
        TeamRules.PreparedName prepared = TeamRules.prepareCreate(rawName, rawTag, s.tagMax);
        TeamRules.Denial denial = TeamRules.createDenial(player.hasPermission("donutteams.create"),
                cache.byPlayer(player.getUniqueId()) != null, prepared, cache.byName(prepared.name()) != null,
                s.nameMin, s.nameMax, s.tagMin, s.tagMax, s.blockedNames);
        if (denial != null) {
            sendDenial(player, denial, "team", prepared.name());
            return;
        }
        double cost = s.createCost;
        boolean charge = cost > 0 && economy.available() && !player.hasPermission("donutteams.bypass.cost");
        if (charge && !economy.has(player, cost)) {
            messages.send(player, "create.cannot-afford", "cost", format.money(cost));
            return;
        }
        TeamCreateEvent event = new TeamCreateEvent(player, prepared.name(), prepared.tag());
        if (!call(event)) {
            messages.send(player, "error.cancelled");
            return;
        }
        long now = System.currentTimeMillis();
        Team team = new Team(UUID.randomUUID(), prepared.name(), prepared.tag(), player.getUniqueId(), now);
        TeamMember owner = new TeamMember(player.getUniqueId(), display(player), TeamRole.OWNER, TeamPermission.all(), now);
        team.putMember(owner);
        team.friendlyFire(s.defaultFriendlyFire);
        team.allyFriendlyFire(s.defaultAllyFriendlyFire);
        team.open(s.defaultOpen);
        team.color(TeamColor.GOLD);
        team.memberCap(slotNode(player));
        lock.lock();
        try {
            if (cache.byPlayer(player.getUniqueId()) != null) {
                messages.send(player, "error.already-in-team");
                return;
            }
            if (cache.byName(team.name()) != null) {
                messages.send(player, "create.exists", "team", team.name());
                return;
            }
            if (charge && !economy.withdraw(player, cost)) {
                messages.send(player, "create.cannot-afford", "cost", format.money(cost));
                return;
            }
            cache.put(team);
            cache.removeInvitesFor(player.getUniqueId());
            removeRequestsFor(player.getUniqueId());
        } finally {
            lock.unlock();
        }
        writer.write("new team " + team.name(), repository -> {
            repository.insertTeam(team, owner);
            repository.deleteInvitesFor(player.getUniqueId());
        });
        if (charge) {
            messages.send(player, "create.paid", "cost", format.money(cost));
        }
        messages.send(player, "create.created", format.team(team));
        announce("create.announce", format.team(team, "player", display(player)));
        plugin.sounds().play(player, "team-created");
    }

    /** Disbands the player's team: the owner gets the bank balance and the chest contents. */
    public void disband(Player player, boolean confirmed) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "error.owner-only");
            return;
        }
        if (!confirmed) {
            messages.send(player, "disband.confirm", format.team(team,
                    "confirm", messages.button("confirm", "/team disband confirm")));
            return;
        }
        disbandTeam(player, team, false);
    }

    /** Disbands any team. Players get their own team's money and items; for admins they go to the owner if online. */
    public void disbandTeam(CommandSender actor, Team team, boolean admin) {
        if (!call(new TeamDisbandEvent(actor, team.id(), team.name(), admin))) {
            messages.send(actor, "error.cancelled");
            return;
        }
        for (TeamMember member : team.members()) {
            TeamLeaveEvent leave = new TeamLeaveEvent(Bukkit.getPlayer(member.playerId()), member.playerId(), team.id(), team.name(),
                    TeamLeaveEvent.Reason.DISBAND);
            if (!call(leave)) {
                messages.send(actor, "error.cancelled");
                return;
            }
        }
        ChestService chestService = chests;
        if (chestService != null && chestService.inUse(team.id())) {
            messages.send(actor, "chest.in-use-disband");
            return;
        }
        Object[] placeholders = format.team(team);
        double balance = team.balance();
        lock.lock();
        try {
            if (cache.byId(team.id()) == null) {
                return;
            }
            cache.remove(team);
            for (Team other : cache.teams()) {
                other.clearRelation(team.id());
            }
            requests.values().removeIf(request -> request.teamId().equals(team.id()));
            team.balance(0);
        } finally {
            lock.unlock();
        }
        UUID ownerId = team.ownerId();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        if (balance > 0 && economy.available()) {
            if (economy.deposit(owner, balance)) {
                Player online = owner.getPlayer();
                if (online != null) {
                    messages.send(online, "disband.refund", "amount", format.money(balance));
                }
            } else {
                plugin.getLogger().warning("Could not pay " + balance + " from disbanded team " + team.name() + " to its owner.");
            }
        }
        if (chestService != null) {
            chestService.takeContents(team.id()).whenComplete((items, error) -> {
                if (error != null) {
                    plugin.getLogger().log(Level.WARNING, "Could not read the chest of disbanded team " + team.name(), error);
                } else if (!items.isEmpty()) {
                    Player online = Bukkit.getPlayer(ownerId);
                    if (online != null) {
                        scheduler.runForEntity(online, () -> {
                            giveOrDrop(online, items);
                            messages.send(online, "disband.chest-returned", "count", String.valueOf(items.size()));
                        });
                    } else {
                        plugin.getLogger().info("Team " + team.name() + " was disbanded with " + items.size()
                                + " stacks in its chest while its owner was offline; they were removed.");
                    }
                }
                writer.write("disbanded team " + team.name(), repository -> repository.deleteTeam(team.id()));
            });
        } else {
            writer.write("disbanded team " + team.name(), repository -> repository.deleteTeam(team.id()));
        }
        for (TeamMember member : team.members()) {
            Player online = Bukkit.getPlayer(member.playerId());
            if (online != null) {
                scheduler.forPlayer(online, () -> {
                    plugin.chat().clearTeamState(online.getUniqueId());
                    plugin.homes().cancel(online, false);
                    messages.send(online, "disband.disbanded", placeholders);
                    plugin.sounds().play(online, "team-disbanded");
                });
            }
        }
        if (admin) {
            messages.send(actor, "admin.disbanded", placeholders);
        }
        announce("disband.announce", placeholders);
    }

    // ── Invites and join requests ───────────────────────────────────────────────────────────────────────────

    public void invite(Player player, Player target) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        refreshMemberCap(team);
        int max = format.maxMembers(team);
        TeamRules.Denial denial = TeamRules.inviteDenial(true, member != null && member.has(TeamPermission.INVITE),
                target.getUniqueId().equals(player.getUniqueId()),
                cache.byPlayer(target.getUniqueId()) != null || cache.invite(team.id(), target.getUniqueId()) != null,
                team.size(), max);
        if (denial != null) {
            sendDenial(player, denial, "player", target.getName(), "max", String.valueOf(max), "permission",
                    format.permission(TeamPermission.INVITE));
            return;
        }
        TeamInvite invite = new TeamInvite(team.id(), target.getUniqueId(), player.getUniqueId(),
                System.currentTimeMillis() + settings.get().inviteExpireSeconds * 1000L);
        cache.putInvite(invite);
        // An invite answers a pending request from the same player.
        requests.remove(requestKey(team.id(), target.getUniqueId()));
        writer.write("invite", repository -> repository.saveInvite(invite));
        messages.send(player, "invite.sent", format.team(team, "player", target.getName()));
        String inviter = display(player);
        scheduler.forPlayer(target, () -> {
            messages.send(target, "invite.received", format.team(team, "player", inviter,
                    "seconds", String.valueOf(settings.get().inviteExpireSeconds),
                    "accept", messages.button("accept-invite", "/team join " + team.name(), "team", team.name()),
                    "decline", messages.button("decline-invite", "/team decline " + team.name(), "team", team.name())));
            plugin.sounds().play(target, "invite-received");
        });
    }

    public void revokeInvite(Player player, UUID targetId) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.INVITE)) {
            return;
        }
        if (cache.invite(team.id(), targetId) == null) {
            return;
        }
        cache.removeInvite(team.id(), targetId);
        writer.write("invite removal", repository -> repository.deleteInvite(team.id(), targetId));
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        messages.send(player, "invite.revoked", "player", target.getName() == null ? targetId.toString() : target.getName());
    }

    /** Joins a team the player was invited to, or an open team. */
    public void join(Player player, String teamName) {
        Team team = cache.byName(teamName);
        TeamInvite invite = team == null ? null : cache.invite(team.id(), player.getUniqueId());
        if (team != null) {
            refreshMemberCap(team);
        }
        int max = team == null ? 0 : format.maxMembers(team);
        TeamRules.Denial denial = TeamRules.joinDenial(cache.byPlayer(player.getUniqueId()) != null, team != null, invite != null,
                false, team != null && team.open(), team == null ? 0 : team.size(), max);
        if (denial == TeamRules.Denial.NO_INVITE && team != null) {
            messages.send(player, "join.no-invite", format.team(team,
                    "request", messages.button("request", "/team request " + team.name(), "team", team.name())));
            return;
        }
        if (denial != null) {
            sendDenial(player, denial, "team", teamName);
            return;
        }
        addMember(player, team, invite == null ? null : invite.invitedBy(), null);
    }

    public void decline(Player player, String teamName) {
        Team team = cache.byName(teamName);
        if (team == null || cache.invite(team.id(), player.getUniqueId()) == null) {
            messages.send(player, "join.no-invite-plain", "team", teamName);
            return;
        }
        cache.removeInvite(team.id(), player.getUniqueId());
        writer.write("invite removal", repository -> repository.deleteInvite(team.id(), player.getUniqueId()));
        messages.send(player, "invite.declined", format.team(team));
        broadcastTo(team, TeamPermission.INVITE, "invite.declined-notify", format.team(team, "player", display(player)));
    }

    /** Adds a player to a team; {@code accepter} is set when an officer accepted a join request. */
    private boolean addMember(Player player, Team team, UUID invitedBy, Player accepter) {
        if (!call(new TeamJoinEvent(player, team.id(), team.name()))) {
            messages.send(player, "error.cancelled");
            return false;
        }
        long now = System.currentTimeMillis();
        TeamMember joining = new TeamMember(player.getUniqueId(), display(player), TeamRole.MEMBER,
                settings.get().memberPermissions, now);
        lock.lock();
        try {
            if (cache.byId(team.id()) == null) {
                messages.send(player, "error.team-not-found", "team", team.name());
                return false;
            }
            if (cache.byPlayer(player.getUniqueId()) != null) {
                messages.send(player, "error.already-in-team");
                return false;
            }
            if (team.size() >= format.maxMembers(team)) {
                messages.send(player, "join.full", format.team(team));
                return false;
            }
            team.putMember(joining);
            cache.indexPlayer(player.getUniqueId(), team.id());
            cache.removeInvitesFor(player.getUniqueId());
            removeRequestsFor(player.getUniqueId());
        } finally {
            lock.unlock();
        }
        writer.write("new member", repository -> {
            repository.saveMember(team.id(), joining);
            repository.deleteInvitesFor(player.getUniqueId());
        });
        Object[] placeholders = format.team(team, "player", display(player));
        messages.send(player, "join.welcome", placeholders);
        broadcastExcept(team, player.getUniqueId(), "join.joined", placeholders);
        plugin.sounds().play(player, "team-joined");
        if (!team.motd().isEmpty()) {
            messages.send(player, "motd.show", placeholders);
        }
        return true;
    }

    /** Asks to join a team that is not open; officers with INVITE are told. */
    public void request(Player player, String teamName) {
        Team team = cache.byName(teamName);
        if (team == null) {
            messages.send(player, "error.team-not-found", "team", teamName);
            return;
        }
        if (cache.byPlayer(player.getUniqueId()) != null) {
            messages.send(player, "error.already-in-team");
            return;
        }
        if (team.open() || cache.invite(team.id(), player.getUniqueId()) != null) {
            join(player, team.name());
            return;
        }
        String key = requestKey(team.id(), player.getUniqueId());
        JoinRequest existing = requests.get(key);
        long now = System.currentTimeMillis();
        if (existing != null && !existing.expired(now)) {
            messages.send(player, "request.already", format.team(team));
            return;
        }
        JoinRequest request = new JoinRequest(team.id(), player.getUniqueId(), player.getName(),
                now + settings.get().requestExpireSeconds * 1000L);
        requests.put(key, request);
        messages.send(player, "request.sent", format.team(team));
        broadcastTo(team, TeamPermission.INVITE, "request.received", format.team(team, "player", player.getName(),
                "accept", messages.button("accept-request", "/team accept " + player.getName(), "player", player.getName()),
                "deny", messages.button("deny-request", "/team deny " + player.getName(), "player", player.getName())));
    }

    public List<JoinRequest> requests(UUID teamId) {
        long now = System.currentTimeMillis();
        requests.values().removeIf(request -> request.expired(now));
        List<JoinRequest> out = new ArrayList<>();
        for (JoinRequest request : requests.values()) {
            if (request.teamId().equals(teamId)) {
                out.add(request);
            }
        }
        out.sort((a, b) -> Long.compare(a.expiresAtMillis(), b.expiresAtMillis()));
        return out;
    }

    public void acceptRequest(Player officer, String playerName) {
        Team team = requireTeam(officer);
        if (team == null || !requirePermission(officer, team, TeamPermission.INVITE)) {
            return;
        }
        JoinRequest request = findRequest(team, playerName);
        if (request == null) {
            messages.send(officer, "request.none", "player", playerName);
            return;
        }
        requests.remove(requestKey(team.id(), request.playerId()));
        Player target = Bukkit.getPlayer(request.playerId());
        if (target == null) {
            messages.send(officer, "error.player-offline", "player", request.playerName());
            return;
        }
        scheduler.forPlayer(target, () -> {
            if (addMember(target, team, officer.getUniqueId(), officer)) {
                messages.send(target, "request.accepted", format.team(team));
            }
        });
    }

    public void denyRequest(Player officer, String playerName) {
        Team team = requireTeam(officer);
        if (team == null || !requirePermission(officer, team, TeamPermission.INVITE)) {
            return;
        }
        JoinRequest request = findRequest(team, playerName);
        if (request == null) {
            messages.send(officer, "request.none", "player", playerName);
            return;
        }
        requests.remove(requestKey(team.id(), request.playerId()));
        messages.send(officer, "request.denied", format.team(team, "player", request.playerName()));
        Player target = Bukkit.getPlayer(request.playerId());
        if (target != null) {
            scheduler.forPlayer(target, () -> messages.send(target, "request.denied-target", format.team(team)));
        }
    }

    private JoinRequest findRequest(Team team, String playerName) {
        for (JoinRequest request : requests(team.id())) {
            if (request.playerName().equalsIgnoreCase(playerName)) {
                return request;
            }
        }
        return null;
    }

    private void removeRequestsFor(UUID playerId) {
        requests.values().removeIf(request -> request.playerId().equals(playerId));
    }

    private static String requestKey(UUID teamId, UUID playerId) {
        return teamId + ":" + playerId;
    }

    // ── Leaving and managing members ────────────────────────────────────────────────────────────────────────

    public void leave(Player player) {
        Team team = cache.byPlayer(player.getUniqueId());
        TeamRules.Denial denial = TeamRules.leaveDenial(team != null, team != null && team.ownerId().equals(player.getUniqueId()));
        if (denial != null) {
            sendDenial(player, denial);
            return;
        }
        if (!call(new TeamLeaveEvent(player, player.getUniqueId(), team.id(), team.name(), TeamLeaveEvent.Reason.LEAVE))) {
            messages.send(player, "error.cancelled");
            return;
        }
        removeMember(team, player.getUniqueId());
        plugin.chat().clearTeamState(player.getUniqueId());
        plugin.homes().cancel(player, false);
        Object[] placeholders = format.team(team, "player", display(player));
        messages.send(player, "leave.left", placeholders);
        broadcast(team, "leave.broadcast", placeholders);
    }

    public void kick(Player player, String targetName) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.KICK)) {
            return;
        }
        TeamMember actor = team.member(player.getUniqueId());
        TeamMember target = findMember(team, targetName);
        TeamRules.Denial denial = TeamRules.manageDenial(actor.role(), target == null ? null : target.role(),
                target != null && target.playerId().equals(player.getUniqueId()));
        if (denial != null) {
            sendDenial(player, denial, "player", target == null ? targetName : target.name());
            return;
        }
        kickMember(player, team, target, TeamLeaveEvent.Reason.KICK);
    }

    private void kickMember(CommandSender actor, Team team, TeamMember target, TeamLeaveEvent.Reason reason) {
        Player online = Bukkit.getPlayer(target.playerId());
        if (!call(new TeamLeaveEvent(online, target.playerId(), team.id(), team.name(), reason))) {
            messages.send(actor, "error.cancelled");
            return;
        }
        removeMember(team, target.playerId());
        Object[] placeholders = format.team(team, "player", target.name());
        broadcast(team, "kick.kicked", placeholders);
        if (!(actor instanceof Player player) || !team.isMember(player.getUniqueId())) {
            messages.send(actor, "kick.kicked", placeholders);
        }
        if (online != null) {
            scheduler.forPlayer(online, () -> {
                plugin.chat().clearTeamState(online.getUniqueId());
                plugin.homes().cancel(online, false);
                messages.send(online, "kick.target", placeholders);
            });
        }
    }

    private void removeMember(Team team, UUID playerId) {
        lock.lock();
        try {
            team.removeMember(playerId);
            cache.unindexPlayer(playerId);
        } finally {
            lock.unlock();
        }
        writer.write("member removal", repository -> repository.deleteMember(playerId));
    }

    public void promote(Player player, String targetName) {
        changeRank(player, targetName, true);
    }

    public void demote(Player player, String targetName) {
        changeRank(player, targetName, false);
    }

    private void changeRank(Player player, String targetName, boolean up) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "error.owner-only");
            return;
        }
        TeamMember actor = team.member(player.getUniqueId());
        TeamMember target = findMember(team, targetName);
        TeamRules.Denial denial = TeamRules.manageDenial(actor.role(), target == null ? null : target.role(),
                target != null && target.playerId().equals(player.getUniqueId()));
        if (denial != null) {
            sendDenial(player, denial, "player", target == null ? targetName : target.name());
            return;
        }
        TeamRole next = up ? TeamRules.promoted(target.role()) : TeamRules.demoted(target.role());
        if (next == null) {
            messages.send(player, up ? "promote.highest" : "demote.lowest", "player", target.name());
            return;
        }
        if (!call(new TeamRoleChangeEvent(team.id(), team.name(), target.playerId(), target.role(), next))) {
            messages.send(player, "error.cancelled");
            return;
        }
        lock.lock();
        try {
            target.role(next);
            target.permissions(settings.get().defaultPermissions(next));
        } finally {
            lock.unlock();
        }
        writer.write("rank change", repository -> repository.saveMember(team.id(), target));
        broadcast(team, up ? "promote.promoted" : "demote.demoted", format.team(team, "player", target.name(),
                "role", format.roleComponent(next)));
    }

    /** Hands the team to another member; the old owner becomes an officer. */
    public void transfer(Player player, String targetName, boolean confirmed) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "error.owner-only");
            return;
        }
        TeamMember target = findMember(team, targetName);
        if (target == null || target.playerId().equals(player.getUniqueId())) {
            messages.send(player, "error.not-a-member", "player", targetName);
            return;
        }
        if (!confirmed) {
            messages.send(player, "transfer.confirm", format.team(team, "player", target.name(),
                    "confirm", messages.button("confirm", "/team transfer " + target.name() + " confirm")));
            return;
        }
        setOwner(player, team, target);
    }

    private void setOwner(CommandSender actor, Team team, TeamMember target) {
        TeamMember oldOwner = team.owner();
        if (!call(new TeamRoleChangeEvent(team.id(), team.name(), target.playerId(), target.role(), TeamRole.OWNER))) {
            messages.send(actor, "error.cancelled");
            return;
        }
        lock.lock();
        try {
            if (oldOwner != null) {
                oldOwner.role(TeamRole.OFFICER);
                oldOwner.permissions(settings.get().defaultPermissions(TeamRole.OFFICER));
            }
            target.role(TeamRole.OWNER);
            target.permissions(TeamPermission.all());
            team.ownerId(target.playerId());
            Player online = Bukkit.getPlayer(target.playerId());
            team.memberCap(online == null ? 0 : slotNode(online));
        } finally {
            lock.unlock();
        }
        writer.write("ownership change", repository -> repository.saveTeamAndMembers(team));
        broadcast(team, "transfer.transferred", format.team(team, "player", target.name()));
        if (!(actor instanceof Player player) || !team.isMember(player.getUniqueId())) {
            messages.send(actor, "transfer.transferred", format.team(team, "player", target.name()));
        }
    }

    public void togglePermission(Player owner, UUID targetId, TeamPermission permission) {
        Team team = requireTeam(owner);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(owner.getUniqueId())) {
            messages.send(owner, "error.owner-only");
            return;
        }
        TeamMember target = team.member(targetId);
        if (target == null || target.isOwner()) {
            return;
        }
        lock.lock();
        try {
            EnumSet<TeamPermission> next = EnumSet.noneOf(TeamPermission.class);
            next.addAll(target.permissions());
            if (!next.add(permission)) {
                next.remove(permission);
            }
            target.permissions(next);
        } finally {
            lock.unlock();
        }
        writer.write("member permissions", repository -> repository.saveMember(team.id(), target));
    }

    // ── Team settings ───────────────────────────────────────────────────────────────────────────────────────

    public void togglePvp(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.PVP)) {
            return;
        }
        boolean next = !team.friendlyFire();
        team.friendlyFire(next);
        saveTeam(team);
        broadcast(team, next ? "pvp.enabled" : "pvp.disabled", format.team(team, "player", display(player)));
    }

    public void toggleAllyPvp(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.PVP)) {
            return;
        }
        boolean next = !team.allyFriendlyFire();
        team.allyFriendlyFire(next);
        saveTeam(team);
        broadcast(team, next ? "pvp.ally-enabled" : "pvp.ally-disabled", format.team(team, "player", display(player)));
    }

    public void toggleOpen(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.SETTINGS)) {
            return;
        }
        boolean next = !team.open();
        team.open(next);
        saveTeam(team);
        broadcast(team, next ? "settings.opened" : "settings.closed", format.team(team, "player", display(player)));
    }

    public void rename(Player player, String newName) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (!team.ownerId().equals(player.getUniqueId())) {
            messages.send(player, "error.owner-only");
            return;
        }
        renameTeam(player, team, newName, team.tag());
    }

    public void retag(Player player, String newTag) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.SETTINGS)) {
            return;
        }
        renameTeam(player, team, team.name(), newTag);
    }

    /** Renames and/or retags (players and admins). */
    public boolean renameTeam(CommandSender actor, Team team, String rawName, String rawTag) {
        Settings s = settings.get();
        TeamRules.PreparedName prepared = new TeamRules.PreparedName(TeamNameValidator.normalizeName(rawName),
                TeamNameValidator.normalizeTag(rawTag));
        TeamRules.Denial denial = TeamRules.nameDenial(prepared, s.nameMin, s.nameMax, s.tagMin, s.tagMax, s.blockedNames);
        if (denial != null) {
            sendDenial(actor, denial, "team", prepared.name());
            return false;
        }
        Team other = cache.byName(prepared.name());
        if (other != null && !other.id().equals(team.id())) {
            messages.send(actor, "create.exists", "team", prepared.name());
            return false;
        }
        String oldName = team.name();
        if (!call(new TeamRenameEvent(team.id(), oldName, prepared.name(), team.tag(), prepared.tag()))) {
            messages.send(actor, "error.cancelled");
            return false;
        }
        lock.lock();
        try {
            team.name(prepared.name());
            team.tag(prepared.tag());
            cache.rename(team, oldName);
        } finally {
            lock.unlock();
        }
        saveTeam(team);
        broadcast(team, "settings.renamed", format.team(team, "old", oldName));
        if (!(actor instanceof Player player) || !team.isMember(player.getUniqueId())) {
            messages.send(actor, "settings.renamed", format.team(team, "old", oldName));
        }
        return true;
    }

    public void setDescription(Player player, String text) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.SETTINGS)) {
            return;
        }
        team.description(clip(text, 120));
        saveTeam(team);
        messages.send(player, "settings.description", format.team(team));
    }

    public void setMotd(Player player, String text) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.SETTINGS)) {
            return;
        }
        team.motd(clip(text, 160));
        saveTeam(team);
        broadcast(team, "settings.motd", format.team(team, "player", display(player)));
    }

    public void setColor(Player player, TeamColor color) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.SETTINGS)) {
            return;
        }
        team.color(color);
        saveTeam(team);
        messages.send(player, "settings.color", format.team(team));
    }

    /** The item in the player's main hand becomes the team icon (a banner looks best). */
    public void setIcon(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requirePermission(player, team, TeamPermission.SETTINGS)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            messages.send(player, "settings.icon-empty");
            return;
        }
        ItemStack icon = hand.clone();
        icon.setAmount(1);
        team.iconData(ItemCodec.encode(icon));
        saveTeam(team);
        messages.send(player, "settings.icon", format.team(team, "item", icon.getType().getKey().getKey().replace('_', ' ')));
    }

    public void saveTeam(Team team) {
        writer.write("team " + team.name(), repository -> repository.saveTeam(team));
    }

    // ── Admin ───────────────────────────────────────────────────────────────────────────────────────────────

    public void adminJoin(CommandSender actor, Player player, Team team) {
        Team current = cache.byPlayer(player.getUniqueId());
        if (current != null) {
            messages.send(actor, "admin.already-in-team", "player", player.getName(), "team", current.name());
            return;
        }
        scheduler.forPlayer(player, () -> {
            if (addMember(player, team, null, null)) {
                messages.send(actor, "admin.joined", format.team(team, "player", player.getName()));
            }
        });
    }

    public void adminKick(CommandSender actor, String playerName) {
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(playerName);
        Player online = Bukkit.getPlayerExact(playerName);
        UUID id = online != null ? online.getUniqueId() : offline == null ? null : offline.getUniqueId();
        Team team = id == null ? null : cache.byPlayer(id);
        if (team == null) {
            messages.send(actor, "admin.not-in-team", "player", playerName);
            return;
        }
        TeamMember target = team.member(id);
        if (target.isOwner()) {
            messages.send(actor, "admin.kick-owner", format.team(team, "player", target.name()));
            return;
        }
        kickMember(actor, team, target, TeamLeaveEvent.Reason.ADMIN);
    }

    public void adminSetOwner(CommandSender actor, Team team, String playerName) {
        TeamMember target = findMember(team, playerName);
        if (target == null) {
            messages.send(actor, "error.not-a-member", "player", playerName);
            return;
        }
        if (target.isOwner()) {
            messages.send(actor, "admin.already-owner", "player", target.name());
            return;
        }
        setOwner(actor, team, target);
    }

    // ── Player state ────────────────────────────────────────────────────────────────────────────────────────

    /** On join: name changes, the owner's slot permission, last seen, the team's message of the day. */
    public void onJoin(Player player) {
        Team team = cache.byPlayer(player.getUniqueId());
        if (team == null) {
            List<TeamInvite> invites = cache.invitesFor(player.getUniqueId());
            if (!invites.isEmpty()) {
                messages.send(player, "invite.pending", "count", String.valueOf(invites.size()),
                        "open", messages.button("open-invites", "/team invites"));
            }
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null) {
            return;
        }
        String name = display(player);
        boolean changed = !name.equals(member.name());
        member.name(name);
        member.lastSeenMillis(System.currentTimeMillis());
        if (member.isOwner()) {
            int cap = slotNode(player);
            if (cap != team.memberCap()) {
                team.memberCap(cap);
                saveTeam(team);
            }
        }
        writer.write("member " + name, repository -> repository.saveMember(team.id(), member));
        Settings s = settings.get();
        if (s.motdOnJoin && !team.motd().isEmpty()) {
            messages.send(player, "motd.show", format.team(team));
        }
        if (s.notifyOnline) {
            broadcastExcept(team, player.getUniqueId(), "notify.online", format.team(team, "player", name));
        }
        if (changed && member.isOwner()) {
            saveTeam(team);
        }
    }

    public void onQuit(Player player) {
        Team team = cache.byPlayer(player.getUniqueId());
        TeamMember member = team == null ? null : team.member(player.getUniqueId());
        if (member == null) {
            return;
        }
        member.lastSeenMillis(System.currentTimeMillis());
        writer.write("member " + member.name(), repository -> repository.saveMember(team.id(), member));
        if (settings.get().notifyOffline) {
            broadcastExcept(team, player.getUniqueId(), "notify.offline", format.team(team, "player", member.name()));
        }
    }

    /** Counts a kill. Kills of teammates and allies are not counted; nor are deaths to them. */
    public void recordKill(Player killer, Player victim) {
        if (!settings.get().statsEnabled || killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        Relation relation = relation(killer.getUniqueId(), victim.getUniqueId());
        if (relation == Relation.TEAMMATE || relation == Relation.ALLY) {
            return;
        }
        Team killerTeam = cache.byPlayer(killer.getUniqueId());
        TeamMember killerMember = killerTeam == null ? null : killerTeam.member(killer.getUniqueId());
        if (killerMember != null) {
            killerMember.addKill();
            writer.write("kill", repository -> repository.saveMember(killerTeam.id(), killerMember));
        }
        Team victimTeam = cache.byPlayer(victim.getUniqueId());
        TeamMember victimMember = victimTeam == null ? null : victimTeam.member(victim.getUniqueId());
        if (victimMember != null) {
            victimMember.addDeath();
            writer.write("death", repository -> repository.saveMember(victimTeam.id(), victimMember));
        }
    }

    /** Keeps the owner's donutteams.slots.<n> on the team while the owner is online. */
    public void refreshMemberCap(Team team) {
        Player owner = Bukkit.getPlayer(team.ownerId());
        if (owner == null) {
            return;
        }
        int cap = slotNode(owner);
        if (cap != team.memberCap()) {
            team.memberCap(cap);
            saveTeam(team);
        }
    }

    private static int slotNode(Player player) {
        Set<String> nodes = new HashSet<>();
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getValue() && info.getPermission().startsWith(SlotPermissionParser.PREFIX)) {
                nodes.add(info.getPermission());
            }
        }
        return SlotPermissionParser.highest(nodes);
    }

    // ── Messaging ───────────────────────────────────────────────────────────────────────────────────────────

    public void broadcast(Team team, String path, Object... keyValues) {
        broadcastExcept(team, null, path, keyValues);
    }

    public void broadcastExcept(Team team, UUID except, String path, Object... keyValues) {
        for (TeamMember member : team.members()) {
            if (member.playerId().equals(except)) {
                continue;
            }
            Player online = Bukkit.getPlayer(member.playerId());
            if (online != null) {
                scheduler.forPlayer(online, () -> messages.send(online, path, keyValues));
            }
        }
    }

    /** To online members who have a team permission. */
    public void broadcastTo(Team team, TeamPermission permission, String path, Object... keyValues) {
        for (TeamMember member : team.members()) {
            if (!member.has(permission)) {
                continue;
            }
            Player online = Bukkit.getPlayer(member.playerId());
            if (online != null) {
                scheduler.forPlayer(online, () -> {
                    messages.send(online, path, keyValues);
                    plugin.sounds().play(online, "notify");
                });
            }
        }
    }

    /** To every online player (an empty message switches it off). */
    public void announce(String path, Object... keyValues) {
        if (messages.isEmpty(path)) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            scheduler.forPlayer(online, () -> messages.send(online, path, keyValues));
        }
        messages.send(Bukkit.getConsoleSender(), path, keyValues);
    }

    public Team requireTeam(Player player) {
        Team team = cache.byPlayer(player.getUniqueId());
        if (team == null) {
            messages.send(player, "error.not-in-team", "create", messages.button("create", "/team"));
        }
        return team;
    }

    public boolean requirePermission(Player player, Team team, TeamPermission permission) {
        TeamMember member = team.member(player.getUniqueId());
        if (member == null || !member.has(permission)) {
            messages.send(player, "error.no-team-permission", "permission", format.permission(permission));
            return false;
        }
        return true;
    }

    public void sendDenial(CommandSender sender, TeamRules.Denial denial, Object... keyValues) {
        Settings s = settings.get();
        Object[] limits = Format.concat(keyValues, "min", String.valueOf(s.nameMin), "max_length", String.valueOf(s.nameMax),
                "tag_min", String.valueOf(s.tagMin), "tag_max", String.valueOf(s.tagMax));
        String path = switch (denial) {
            case NO_CREATE_PERMISSION -> "create.no-permission";
            case ALREADY_IN_TEAM -> "error.already-in-team";
            case INVALID_NAME -> "create.invalid-name";
            case INVALID_TAG -> "create.invalid-tag";
            case BLOCKED_NAME -> "create.blocked";
            case NAME_TAKEN -> "create.exists";
            case NOT_IN_TEAM -> "error.not-in-team";
            case NO_INVITE_PERMISSION -> "error.no-team-permission";
            case INVITE_SELF -> "invite.self";
            case ALREADY_INVITED_OR_MEMBER -> "invite.already";
            case TEAM_FULL -> "invite.full";
            case OWNER_CANNOT_LEAVE -> "leave.owner";
            case TEAM_NOT_FOUND -> "error.team-not-found";
            case NO_INVITE -> "join.no-invite-plain";
            case INVITE_EXPIRED -> "join.expired";
            case NOT_A_MEMBER -> "error.not-a-member";
            case CANNOT_MANAGE_SELF -> "error.manage-self";
            case RANK_TOO_LOW -> "error.rank-too-low";
            case ALREADY_HIGHEST -> "promote.highest";
            case ALREADY_LOWEST -> "demote.lowest";
        };
        messages.send(sender, path, limits);
    }

    /** Fires a Bukkit event; returns false when it was cancelled. */
    public static boolean call(Event event) {
        Bukkit.getPluginManager().callEvent(event);
        return !(event instanceof org.bukkit.event.Cancellable cancellable) || !cancellable.isCancelled();
    }

    public void giveOrDrop(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(item);
            for (ItemStack overflow : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
    }

    private static String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.equalsIgnoreCase("clear") || trimmed.equalsIgnoreCase("none") || trimmed.equals("-")) {
            return "";
        }
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    public Component none() {
        return messages.component("format.none");
    }

    static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
