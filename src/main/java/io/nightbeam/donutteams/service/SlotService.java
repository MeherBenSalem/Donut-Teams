package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.config.PluginSettings;
import io.nightbeam.donutteams.hook.LuckPermsHook;
import io.nightbeam.donutteams.util.SlotPermissionParser;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class SlotService {

    private final PluginSettings settings;
    private final LuckPermsHook luckPermsHook;

    public SlotService(PluginSettings settings, LuckPermsHook luckPermsHook) {
        this.settings = settings;
        this.luckPermsHook = luckPermsHook;
    }

    public int maxMembers(Player ownerOnline) {
        if (ownerOnline != null) {
            return SlotPermissionParser.parseHighest(effectiveNodes(ownerOnline), settings.defaultMaxMembers(), settings.liteMaxMembers());
        }
        return Math.min(settings.defaultMaxMembers(), settings.liteMaxMembers());
    }

    public int maxMembers(UUID ownerId) {
        Player online = Bukkit.getPlayer(ownerId);
        if (online != null) {
            return maxMembers(online);
        }
        Set<String> fromLuckPerms = luckPermsHook.slotNodes(ownerId);
        if (!fromLuckPerms.isEmpty()) {
            return SlotPermissionParser.parseHighest(fromLuckPerms, settings.defaultMaxMembers(), settings.liteMaxMembers());
        }
        return Math.min(settings.defaultMaxMembers(), settings.liteMaxMembers());
    }

    private static Set<String> effectiveNodes(Player player) {
        Set<String> nodes = new HashSet<>();
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getValue()) {
                nodes.add(info.getPermission());
            }
        }
        return nodes;
    }
}
