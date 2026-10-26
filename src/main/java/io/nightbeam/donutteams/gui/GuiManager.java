package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.service.ChestService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Opens menus and routes their clicks; also watches team chests for saving. */
public final class GuiManager implements Listener {

    private final DonutTeamsPlugin plugin;
    private final MenuConfig config = new MenuConfig();

    public GuiManager(DonutTeamsPlugin plugin) {
        this.plugin = plugin;
    }

    public MenuConfig config() {
        return config;
    }

    /** The team menu, or the "no team" menu. */
    public void openMain(Player player) {
        Team team = plugin.teams().teamByPlayer(player.getUniqueId());
        if (team == null) {
            new NoTeamMenu(plugin, player).open();
        } else {
            new MainMenu(plugin, player).open();
        }
    }

    private boolean inTeam(Player player) {
        return plugin.teams().requireTeam(player) != null;
    }

    public void openMembers(Player player) {
        if (inTeam(player)) {
            new MembersMenu(plugin, player).open();
        }
    }

    public void openInvites(Player player) {
        new InvitesMenu(plugin, player).open();
    }

    public void openRequests(Player player) {
        if (inTeam(player)) {
            new RequestsMenu(plugin, player).open();
        }
    }

    public void openHomes(Player player) {
        if (inTeam(player)) {
            new HomesMenu(plugin, player).open();
        }
    }

    public void openBank(Player player) {
        if (inTeam(player)) {
            new BankMenu(plugin, player).open();
        }
    }

    public void openLevels(Player player) {
        if (inTeam(player)) {
            new LevelMenu(plugin, player).open();
        }
    }

    public void openRelations(Player player) {
        if (inTeam(player)) {
            new RelationsMenu(plugin, player).open();
        }
    }

    public void openSettings(Player player) {
        if (inTeam(player)) {
            new SettingsMenu(plugin, player).open();
        }
    }

    public void openColors(Player player) {
        if (inTeam(player)) {
            new ColorMenu(plugin, player).open();
        }
    }

    public void openBrowse(Player player) {
        new BrowseMenu(plugin, player).open();
    }

    public void openTop(Player player, io.nightbeam.donutteams.service.TopService.Stat stat) {
        new TopMenu(plugin, player, stat).open();
    }

    public void openProfile(Player player, Team team) {
        new ProfileMenu(plugin, player, team.id(), () -> new BrowseMenu(plugin, player).open()).open();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof ChestService.Holder) {
            plugin.chests().markDirty(event.getView().getTopInventory());
            return;
        }
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (menu.viewer.getUniqueId().equals(player.getUniqueId())) {
            menu.click(event.getRawSlot(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        } else if (event.getView().getTopInventory().getHolder(false) instanceof ChestService.Holder) {
            plugin.chests().markDirty(event.getView().getTopInventory());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        plugin.chests().onClose(event);
    }
}
