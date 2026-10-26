package io.nightbeam.donutteams.hook;

import io.nightbeam.donutteams.util.Amounts;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** The Vault economy. Only loaded when Vault is installed. */
final class VaultEconomy implements Economy {

    private final net.milkbowl.vault.economy.Economy economy;

    private VaultEconomy(net.milkbowl.vault.economy.Economy economy) {
        this.economy = economy;
    }

    /** The registered economy, or null when no economy plugin has registered one yet. */
    static VaultEconomy find() {
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> registration =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (registration == null || registration.getProvider() == null) {
            return null;
        }
        return new VaultEconomy(registration.getProvider());
    }

    @Override
    public boolean available() {
        return economy.isEnabled();
    }

    @Override
    public String name() {
        return economy.getName();
    }

    @Override
    public double balance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!economy.has(player, amount)) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public String format(double amount) {
        try {
            String formatted = economy.format(amount);
            return formatted == null || formatted.isBlank() ? "$" + Amounts.full(amount) : formatted;
        } catch (Throwable ignored) {
            return "$" + Amounts.full(amount);
        }
    }
}
