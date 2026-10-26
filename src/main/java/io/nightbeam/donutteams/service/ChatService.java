package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.api.event.TeamChatEvent;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Team chat and ally chat (a command, a toggle, or a quick prefix), admin chat spy, and chat prompts for menu
 * buttons that need text (a team name, an amount).
 */
public final class ChatService {

    /** A question waiting for the player's next chat line. */
    private record Prompt(Consumer<String> answer, long expiresAtMillis) {
    }

    private static final long PROMPT_MILLIS = 60_000L;

    private final DonutTeamsPlugin plugin;
    private final Supplier<Settings> settings;
    private final Messages messages;
    private final FoliaScheduler scheduler;
    private final TeamService teams;
    private final Set<UUID> teamToggled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> allyToggled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatService(DonutTeamsPlugin plugin, Supplier<Settings> settings, Messages messages, FoliaScheduler scheduler, TeamService teams) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.scheduler = scheduler;
        this.teams = teams;
    }

    // ── Prompts ─────────────────────────────────────────────────────────────────────────────────────────────

    /** Asks the player to type an answer in chat ({@code cancel} cancels). */
    public void prompt(Player player, String promptPath, Consumer<String> answer, Object... keyValues) {
        prompts.put(player.getUniqueId(), new Prompt(answer, System.currentTimeMillis() + PROMPT_MILLIS));
        player.closeInventory();
        messages.send(player, promptPath, keyValues);
        messages.send(player, "prompt.hint", "cancel", messages.button("cancel-prompt", "/team cancel"));
    }

    public boolean hasPrompt(UUID playerId) {
        Prompt prompt = prompts.get(playerId);
        if (prompt != null && prompt.expiresAtMillis() < System.currentTimeMillis()) {
            prompts.remove(playerId, prompt);
            return false;
        }
        return prompt != null;
    }

    public void cancelPrompt(Player player) {
        if (prompts.remove(player.getUniqueId()) != null) {
            messages.send(player, "prompt.cancelled");
        }
    }

    // ── Chat routing ────────────────────────────────────────────────────────────────────────────────────────

    /** Whether a chat line will be handled here (so the public message must be cancelled). Any thread. */
    public boolean wants(Player player, String message) {
        UUID id = player.getUniqueId();
        if (hasPrompt(id)) {
            return true;
        }
        if (!settings.get().chatEnabled) {
            return false;
        }
        Team team = teams.teamByPlayer(id);
        if (team == null) {
            // A toggle left over from a team the player is no longer in must not swallow public chat.
            teamToggled.remove(id);
            allyToggled.remove(id);
            return false;
        }
        Settings s = settings.get();
        return quick(message, s.quickPrefix) != null || (s.allyChat && quick(message, s.allyQuickPrefix) != null)
                || teamToggled.contains(id) || allyToggled.contains(id);
    }

    /** Handles a chat line that {@link #wants} accepted, on the player's thread. */
    public void handle(Player player, String message) {
        Prompt prompt = prompts.remove(player.getUniqueId());
        if (prompt != null && prompt.expiresAtMillis() >= System.currentTimeMillis()) {
            if (message.trim().equalsIgnoreCase("cancel")) {
                messages.send(player, "prompt.cancelled");
                return;
            }
            prompt.answer().accept(message.trim());
            return;
        }
        Settings s = settings.get();
        String quickTeam = quick(message, s.quickPrefix);
        if (quickTeam != null) {
            sendTeam(player, quickTeam);
            return;
        }
        String quickAlly = s.allyChat ? quick(message, s.allyQuickPrefix) : null;
        if (quickAlly != null) {
            sendAlly(player, quickAlly);
            return;
        }
        if (allyToggled.contains(player.getUniqueId())) {
            sendAlly(player, message);
        } else if (teamToggled.contains(player.getUniqueId())) {
            sendTeam(player, message);
        }
    }

    private static String quick(String message, String prefix) {
        if (prefix == null || prefix.isEmpty() || !message.startsWith(prefix)) {
            return null;
        }
        String rest = message.substring(prefix.length()).trim();
        return rest.isEmpty() ? null : rest;
    }

    // ── Team chat ───────────────────────────────────────────────────────────────────────────────────────────

    public boolean teamToggled(UUID playerId) {
        return teamToggled.contains(playerId);
    }

    public boolean allyToggled(UUID playerId) {
        return allyToggled.contains(playerId);
    }

    public void teamChat(Player player, String message) {
        if (!canTeamChat(player)) {
            return;
        }
        if (message == null || message.isBlank()) {
            allyToggled.remove(player.getUniqueId());
            if (teamToggled.remove(player.getUniqueId())) {
                messages.send(player, "chat.disabled");
            } else {
                teamToggled.add(player.getUniqueId());
                messages.send(player, "chat.enabled");
            }
            return;
        }
        sendTeam(player, message);
    }

    public void allyChat(Player player, String message) {
        if (!settings.get().allyChat) {
            messages.send(player, "error.feature-disabled");
            return;
        }
        if (!canTeamChat(player)) {
            return;
        }
        if (message == null || message.isBlank()) {
            teamToggled.remove(player.getUniqueId());
            if (allyToggled.remove(player.getUniqueId())) {
                messages.send(player, "chat.ally-disabled");
            } else {
                allyToggled.add(player.getUniqueId());
                messages.send(player, "chat.ally-enabled");
            }
            return;
        }
        sendAlly(player, message);
    }

    private boolean canTeamChat(Player player) {
        if (!settings.get().chatEnabled || !player.hasPermission("donutteams.chat")) {
            messages.send(player, "error.no-permission");
            return false;
        }
        Team team = teams.requireTeam(player);
        return team != null && teams.requirePermission(player, team, TeamPermission.SPEAK);
    }

    public void sendTeam(Player player, String raw) {
        Team team = teams.teamByPlayer(player.getUniqueId());
        TeamMember member = team == null ? null : team.member(player.getUniqueId());
        if (member == null) {
            teamToggled.remove(player.getUniqueId());
            messages.send(player, "error.not-in-team", "create", messages.button("create", "/team"));
            return;
        }
        if (!member.has(TeamPermission.SPEAK) || !player.hasPermission("donutteams.chat")) {
            messages.send(player, "error.no-team-permission", "permission", teams.format().permission(TeamPermission.SPEAK));
            return;
        }
        TeamChatEvent event = new TeamChatEvent(player, team.id(), team.name(), raw, false);
        if (!TeamService.call(event) || event.getMessage().isBlank()) {
            return;
        }
        Component line = messages.component("chat.format", teams.format().team(team, "player", teams.display(player),
                "role", teams.format().roleComponent(member.role()), "message", event.getMessage()));
        deliver(team.members().stream().map(TeamMember::playerId).toList(), line);
        spy(line, team.members().stream().map(TeamMember::playerId).toList());
        if (settings.get().chatLogToConsole) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    public void sendAlly(Player player, String raw) {
        Team team = teams.teamByPlayer(player.getUniqueId());
        TeamMember member = team == null ? null : team.member(player.getUniqueId());
        if (member == null) {
            allyToggled.remove(player.getUniqueId());
            messages.send(player, "error.not-in-team", "create", messages.button("create", "/team"));
            return;
        }
        if (team.allies().isEmpty()) {
            messages.send(player, "chat.no-allies");
            return;
        }
        TeamChatEvent event = new TeamChatEvent(player, team.id(), team.name(), raw, true);
        if (!TeamService.call(event) || event.getMessage().isBlank()) {
            return;
        }
        Component line = messages.component("chat.ally-format", teams.format().team(team, "player", teams.display(player),
                "role", teams.format().roleComponent(member.role()), "message", event.getMessage()));
        java.util.List<UUID> recipients = new java.util.ArrayList<>();
        team.members().forEach(m -> recipients.add(m.playerId()));
        for (UUID allyId : team.allies()) {
            Team ally = teams.teamById(allyId);
            if (ally != null) {
                ally.members().forEach(m -> recipients.add(m.playerId()));
            }
        }
        deliver(recipients, line);
        spy(line, recipients);
        if (settings.get().chatLogToConsole) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    private void deliver(Iterable<UUID> recipients, Component line) {
        for (UUID id : recipients) {
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
                scheduler.forPlayer(online, () -> {
                    online.sendMessage(line);
                    plugin.sounds().play(online, "chat");
                });
            }
        }
    }

    // ── Spy ─────────────────────────────────────────────────────────────────────────────────────────────────

    public boolean toggleSpy(Player player) {
        if (spies.remove(player.getUniqueId())) {
            return false;
        }
        spies.add(player.getUniqueId());
        return true;
    }

    private void spy(Component line, java.util.List<UUID> recipients) {
        if (spies.isEmpty()) {
            return;
        }
        Component copy = messages.component("chat.spy-format", "message", line);
        for (UUID id : spies) {
            if (recipients.contains(id)) {
                continue;
            }
            Player spy = Bukkit.getPlayer(id);
            if (spy != null && spy.hasPermission("donutteams.admin.spy")) {
                scheduler.forPlayer(spy, () -> spy.sendMessage(copy));
            }
        }
    }

    /** Forgets a player's chat modes after they leave their team (or the server). */
    public void clearTeamState(UUID playerId) {
        teamToggled.remove(playerId);
        allyToggled.remove(playerId);
    }

    public void clear(UUID playerId) {
        clearTeamState(playerId);
        prompts.remove(playerId);
    }
}
