package io.nightbeam.donutteams.hook;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;

public final class LuckPermsHook {

    private final boolean available;

    public LuckPermsHook() {
        this.available = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    public boolean isAvailable() {
        return available;
    }

    @SuppressWarnings("unchecked")
    public Set<String> slotNodes(UUID playerId) {
        if (!available) {
            return Collections.emptySet();
        }
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get").invoke(null);
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            CompletableFuture<?> future = (CompletableFuture<?>) userManager.getClass()
                    .getMethod("loadUser", UUID.class)
                    .invoke(userManager, playerId);
            Object user = future.get(2, TimeUnit.SECONDS);
            if (user == null) {
                return Collections.emptySet();
            }
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Map<String, Boolean> map = (Map<String, Boolean>) permissionData.getClass()
                    .getMethod("getPermissionMap")
                    .invoke(permissionData);
            Set<String> nodes = new HashSet<>();
            map.forEach((key, value) -> {
                if (Boolean.TRUE.equals(value) && key.toLowerCase().startsWith("donutteams.slots.")) {
                    nodes.add(key);
                }
            });
            return nodes;
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }
}
