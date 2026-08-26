package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.PluginSettings;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ChatService {

    private final PluginSettings settings;
    private final Messages messages;
    private final FoliaScheduler scheduler;
    private final TeamService teams;
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> pendingInput = new ConcurrentHashMap<>();

    public ChatService(PluginSettings settings, Messages messages, FoliaScheduler scheduler, TeamService teams) {
        this.settings = settings;
        this.messages = messages;
        this.scheduler = scheduler;
        this.teams = teams;
    }

    public boolean toggled(UUID playerId) {
        return toggled.contains(playerId);
    }

    public void clear(UUID playerId) {
        toggled.remove(playerId);
        pendingInput.remove(playerId);
    }

    public void awaitCreate(Player player) {
        pendingInput.put(player.getUniqueId(), "create");
        messages.send(player, "create.prompt", "<gray>Type a team name in chat, or cancel.");
    }

    public boolean hasPending(UUID playerId) {
        return pendingInput.containsKey(playerId);
    }

    public boolean handleInput(Player player, String message) {
        String pending = pendingInput.remove(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        if (message.equalsIgnoreCase("cancel")) {
            messages.send(player, "cancelled", "<red>Action cancelled.");
            return true;
        }
        if ("create".equals(pending)) {
            String[] parts = message.trim().split("\\s+");
            String name = parts[0];
            String tag = parts.length > 1 ? parts[1] : name;
            teams.create(player, name, tag);
            return true;
        }
        return false;
    }

    public void toggleOrSend(Player player, String message) {
        if (!settings.chatEnabled() || !player.hasPermission("donutteams.chat")) {
            messages.send(player, "general.no-permission", "<red>No permission.");
            return;
        }
        Team team = teams.teamByPlayer(player.getUniqueId());
        if (team == null) {
            messages.send(player, "not-in-team", "<red>You are not in a team.");
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null || !member.has(TeamPermission.SPEAK)) {
            messages.send(player, "chat.no-speak", "<red>No speak.");
            return;
        }
        if (message == null || message.isBlank()) {
            if (toggled.remove(player.getUniqueId())) {
                messages.send(player, "chat.disabled", "<yellow>Team chat disabled.");
            } else {
                toggled.add(player.getUniqueId());
                messages.send(player, "chat.enabled", "<green>Team chat enabled.");
            }
            return;
        }
        send(player, Component.text(message));
    }

    public boolean handleToggledChat(Player player, Component message) {
        if (!toggled(player.getUniqueId())) {
            return false;
        }
        send(player, message);
        return true;
    }

    public void send(Player player, Component rawMessage) {
        Team team = teams.teamByPlayer(player.getUniqueId());
        if (team == null) {
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        if (member == null || !member.has(TeamPermission.SPEAK) || !player.hasPermission("donutteams.chat")) {
            messages.send(player, "chat.no-speak", "<red>No speak.");
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(rawMessage);
        Component formatted = messages.component(
                "chat.format",
                "<dark_gray>[<gold><tag></gold>]</dark_gray> <white><player></white><dark_gray>:</dark_gray> <gray><message>",
                "tag", team.tag(),
                "team", team.name(),
                "player", teams.display(player),
                "message", plain);
        boolean any = false;
        for (TeamMember other : team.members()) {
            Player online = Bukkit.getPlayer(other.playerId());
            if (online == null) {
                continue;
            }
            any = true;
            Player target = online;
            scheduler.runForEntity(target, () -> target.sendMessage(formatted));
        }
        if (!any) {
            messages.send(player, "chat.empty", "<red>Empty.");
        }
    }
}
