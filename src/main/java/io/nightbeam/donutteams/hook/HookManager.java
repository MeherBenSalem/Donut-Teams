package io.nightbeam.donutteams.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class HookManager {

    private HookManager() {
    }

    public static DonutCoreHook donutCore(JavaPlugin plugin) {
        Plugin donutCore = Bukkit.getPluginManager().getPlugin("DonutCore");
        if (donutCore == null || !donutCore.isEnabled()) {
            return new NoopDonutCoreHook();
        }
        plugin.getLogger().info("DonutCore detected, enabling display-name integration.");
        return new ReflectiveDonutCoreHook(donutCore);
    }

    public static boolean present(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }
}
