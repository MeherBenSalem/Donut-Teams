package io.nightbeam.donutteams.util;

import io.nightbeam.donutteams.config.Messages;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/** Menu items: names and lore are never italic, attributes and patterns are hidden. */
public final class ItemBuilder {

    private static final Method GLINT_OVERRIDE = findGlintOverride();

    private ItemBuilder() {
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        return material == null || !material.isItem() || material == Material.AIR ? fallback : material;
    }

    public static ItemStack of(Material material, Component name, List<Component> lore) {
        return decorate(new ItemStack(material), name, lore, false);
    }

    /** Sets name and lore on a copy of {@code base}. */
    public static ItemStack decorate(ItemStack base, Component name, List<Component> lore, boolean glow) {
        ItemStack item = base.clone();
        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), item.getAmount())));
        item.editMeta(meta -> {
            meta.displayName(Messages.item(name));
            meta.lore(lore == null || lore.isEmpty() ? null : Messages.item(lore));
            meta.addItemFlags(ItemFlag.values());
            if (glow) {
                glow(meta);
            }
        });
        return item;
    }

    public static ItemStack head(UUID playerId) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerId)));
        return item;
    }

    public static ItemStack filler(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Messages.item(name));
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }

    private static void glow(ItemMeta meta) {
        if (GLINT_OVERRIDE != null) {
            try {
                GLINT_OVERRIDE.invoke(meta, Boolean.TRUE);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall through to an enchantment.
            }
        }
        try {
            Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
            if (enchantment != null) {
                meta.addEnchant(enchantment, 1, true);
            }
        } catch (Throwable ignored) {
            // Glow is cosmetic.
        }
    }

    /** {@code ItemMeta#setEnchantmentGlintOverride} exists from 1.20.5. */
    private static Method findGlintOverride() {
        try {
            return ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
