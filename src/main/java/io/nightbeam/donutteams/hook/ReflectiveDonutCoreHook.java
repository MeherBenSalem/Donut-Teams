package io.nightbeam.donutteams.hook;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ReflectiveDonutCoreHook implements DonutCoreHook {

    private final Plugin plugin;
    private Method playerServiceAccessor;
    private Method playerDisplayMethod;

    public ReflectiveDonutCoreHook(Plugin plugin) {
        this.plugin = plugin;
        resolveMethods();
    }

    @Override
    public boolean isAvailable() {
        return playerServiceAccessor != null && playerDisplayMethod != null;
    }

    @Override
    public String resolveDisplayName(Player player) {
        if (playerServiceAccessor == null || playerDisplayMethod == null) {
            return player.getName();
        }
        try {
            Object service = playerServiceAccessor.invoke(plugin);
            Object result = playerDisplayMethod.invoke(service, player.getUniqueId());
            return result == null ? player.getName() : String.valueOf(result);
        } catch (ReflectiveOperationException ignored) {
            return player.getName();
        }
    }

    private void resolveMethods() {
        try {
            Class<?> pluginClass = plugin.getClass();
            playerServiceAccessor = pluginClass.getMethod("getPlayerService");
            Class<?> serviceType = playerServiceAccessor.getReturnType();
            playerDisplayMethod = serviceType.getMethod("getDisplayName", UUID.class);
        } catch (ReflectiveOperationException ignored) {
            playerServiceAccessor = null;
            playerDisplayMethod = null;
        }
    }
}
