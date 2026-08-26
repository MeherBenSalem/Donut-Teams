package io.nightbeam.donutteams.hook;

import org.bukkit.entity.Player;

public interface DonutCoreHook {

    boolean isAvailable();

    String resolveDisplayName(Player player);
}
