package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class SettingsGui extends BaseGui {

    private final GuiManager guis;
    private final Messages messages;
    private final TeamService teams;

    public SettingsGui(GuiManager guis, Messages messages, TeamService teams) {
        this.guis = guis;
        this.messages = messages;
        this.teams = teams;
    }

    @Override
    public Inventory render(Player player) {
        Inventory inventory = attach(Bukkit.createInventory(this, 45, messages.component("gui.title-settings", "<gold>Team Settings")));
        TeamMainGui.ItemStackFiller.fill(inventory);
        Team team = teams.teamByPlayer(player.getUniqueId());
        if (team != null) {
            inventory.setItem(10, ItemBuilder.of(Material.NAME_TAG, messages.component("info.header", "<gold><team>",
                    "team", team.name(), "tag", team.tag()), List.of()));
            inventory.setItem(12, ItemBuilder.of(Material.IRON_SWORD, messages.component("gui.pvp-name", "<red>Friendly fire"),
                    List.of(messages.component(team.friendlyFire() ? "gui.pvp-on" : "gui.pvp-off",
                            team.friendlyFire() ? "<green>ON" : "<red>OFF"))));
            inventory.setItem(14, ItemBuilder.of(Material.RED_BED, messages.component("gui.home-name", "<green>Team home"),
                    List.of(messages.deserialize(team.home() == null
                            ? messages.raw("gui.home-unset", "Not set")
                            : "<white>" + team.home().world()))));
            inventory.setItem(16, ItemBuilder.of(Material.PLAYER_HEAD, messages.component("gui.members-name", "<yellow>Members"),
                    List.of(messages.component("info.members", "<gray>Members",
                            "count", String.valueOf(team.size()), "max", "8"))));
        }
        inventory.setItem(28, locked("gui.pro-allies", "<gray>Allies / wars"));
        inventory.setItem(30, locked("gui.pro-bank", "<gray>Team bank"));
        inventory.setItem(32, locked("gui.pro-homes", "<gray>Extra / personal homes"));
        inventory.setItem(34, locked("gui.pro-cosmetics", "<gray>Nametags / tablist"));
        inventory.setItem(40, ItemBuilder.of(Material.ARROW, messages.component("gui.back-name", "<gray>Back"), List.of()));
        return inventory;
    }

    private org.bukkit.inventory.ItemStack locked(String path, String def) {
        return ItemBuilder.of(Material.GRAY_DYE, messages.component(path, def),
                List.of(
                        messages.component("gui.pro-locked", "<dark_gray>Pro"),
                        messages.component("gui.pro-lore", "<dark_gray>Coming in Donut Teams Pro")));
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 40) {
            guis.openMain(player);
            return;
        }
        if (slot == 12) {
            teams.togglePvp(player);
            guis.openSettings(player);
        }
    }
}
