package io.nightbeam.donutteams.hook;

import io.nightbeam.donutteams.util.Amounts;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Finds the Vault economy the first time it is needed and again whenever it is missing, so an economy plugin that
 * enables after Donut Teams is still picked up. Vault classes are only loaded when Vault is installed.
 */
public final class EconomyHook implements Economy {

    private volatile Economy delegate;

    @Override
    public boolean available() {
        return resolve() != null;
    }

    @Override
    public String name() {
        Economy economy = resolve();
        return economy == null ? "none" : economy.name();
    }

    @Override
    public double balance(OfflinePlayer player) {
        Economy economy = resolve();
        return economy == null ? 0 : economy.balance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        Economy economy = resolve();
        return economy != null && economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        Economy economy = resolve();
        return economy != null && economy.withdraw(player, amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        Economy economy = resolve();
        return economy != null && economy.deposit(player, amount);
    }

    @Override
    public String format(double amount) {
        Economy economy = resolve();
        return economy == null ? "$" + Amounts.full(amount) : economy.format(amount);
    }

    private Economy resolve() {
        Economy current = delegate;
        if (current != null && current.available()) {
            return current;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        try {
            Economy vault = VaultEconomy.find();
            delegate = vault;
            return vault;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
