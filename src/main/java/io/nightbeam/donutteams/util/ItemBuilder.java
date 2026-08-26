package io.nightbeam.donutteams.util;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public final class ItemBuilder {

    private ItemBuilder() {
    }

    public static ItemStack of(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        });
        return item;
    }

    public static ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        item.editMeta(meta -> meta.displayName(Component.text(" ")));
        return item;
    }

    public static ItemStack skull(UUID playerId, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(SkullMeta.class, meta -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
            meta.setOwningPlayer(offline);
            meta.displayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
        });
        return item;
    }
}
