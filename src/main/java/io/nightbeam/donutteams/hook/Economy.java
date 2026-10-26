package io.nightbeam.donutteams.hook;

import org.bukkit.OfflinePlayer;

/** Player money, through Vault when an economy plugin is installed. */
public interface Economy {

    boolean available();

    String name();

    double balance(OfflinePlayer player);

    boolean has(OfflinePlayer player, double amount);

    /** Takes money; false when the player cannot afford it or the economy refused. */
    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    String format(double amount);
}
