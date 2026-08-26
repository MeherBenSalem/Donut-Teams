package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.service.ChatService;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class TeamMainGui extends BaseGui {

    private final GuiManager guis;
    private final Messages messages;
    private final TeamService teams;
    private final ChatService chat;

    public TeamMainGui(GuiManager guis, Messages messages, TeamService teams, ChatService chat) {
        this.guis = guis;
        this.messages = messages;
        this.teams = teams;
        this.chat = chat;
    }

    @Override
    public Inventory render(Player player) {
        Inventory inventory = attach(Bukkit.createInventory(this, 45, messages.component("gui.title-main", "<gold>Donut Teams")));
        ItemStackFiller.fill(inventory);
        Team team = teams.teamByPlayer(player.getUniqueId());
        if (team == null) {
            inventory.setItem(22, ItemBuilder.of(
                    Material.EMERALD,
                    messages.component("gui.create-name", "<green>Create team"),
                    messages.list("gui.create-lore")));
            return inventory;
        }
        TeamMember member = team.member(player.getUniqueId());
        boolean owner = member != null && member.isOwner();
        inventory.setItem(10, ItemBuilder.of(Material.BOOK, messages.component("gui.info-name", "<aqua>Team info"),
                List.of(infoLine(team, player))));
        inventory.setItem(12, ItemBuilder.of(Material.PLAYER_HEAD, messages.component("gui.members-name", "<yellow>Members"), List.of()));
        inventory.setItem(14, ItemBuilder.of(Material.WRITABLE_BOOK, messages.component("gui.invites-name", "<gold>Invites"), List.of()));
        inventory.setItem(16, ItemBuilder.of(Material.COMPARATOR, messages.component("gui.settings-name", "<white>Settings"), List.of()));
        inventory.setItem(28, ItemBuilder.of(Material.RED_BED, messages.component("gui.home-name", "<green>Team home"),
                List.of(messages.component("info.home", "<gray>Home", "home",
                        team.home() == null ? messages.raw("gui.home-unset", "Not set") : team.home().world()))));
        inventory.setItem(29, ItemBuilder.of(Material.COMPASS, messages.component("gui.sethome-name", "<aqua>Set home"), List.of()));
        inventory.setItem(30, ItemBuilder.of(Material.BARRIER, messages.component("gui.delhome-name", "<red>Delete home"), List.of()));
        inventory.setItem(32, ItemBuilder.of(Material.GOAT_HORN, messages.component("gui.chat-name", "<light_purple>Team chat"),
                List.of(messages.component(chat.toggled(player.getUniqueId()) ? "gui.chat-on" : "gui.chat-off",
                        chat.toggled(player.getUniqueId()) ? "<green>ON" : "<gray>OFF"))));
        inventory.setItem(33, ItemBuilder.of(Material.IRON_SWORD, messages.component("gui.pvp-name", "<red>Friendly fire"),
                List.of(messages.component(team.friendlyFire() ? "gui.pvp-on" : "gui.pvp-off",
                        team.friendlyFire() ? "<green>ON" : "<red>OFF"))));
        if (owner) {
            inventory.setItem(34, ItemBuilder.of(Material.TNT, messages.component("gui.disband-name", "<dark_red>Disband team"), List.of()));
        } else {
            inventory.setItem(34, ItemBuilder.of(Material.OAK_DOOR, messages.component("gui.leave-name", "<red>Leave team"), List.of()));
        }
        return inventory;
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        Team team = teams.teamByPlayer(player.getUniqueId());
        int slot = event.getRawSlot();
        if (team == null) {
            if (slot == 22) {
                player.closeInventory();
                chat.awaitCreate(player);
            }
            return;
        }
        TeamMember member = team.member(player.getUniqueId());
        switch (slot) {
            case 10 -> {
                player.closeInventory();
                teams.info(player, null);
            }
            case 12 -> guis.openMembers(player);
            case 14 -> guis.openInvites(player);
            case 16 -> guis.openSettings(player);
            case 28 -> {
                player.closeInventory();
                guis.homes().start(player);
            }
            case 29 -> {
                player.closeInventory();
                teams.setHome(player);
            }
            case 30 -> {
                player.closeInventory();
                teams.deleteHome(player);
            }
            case 32 -> {
                player.closeInventory();
                chat.toggleOrSend(player, null);
            }
            case 33 -> {
                teams.togglePvp(player);
                guis.openMain(player);
            }
            case 34 -> {
                player.closeInventory();
                if (member != null && member.isOwner()) {
                    teams.disband(player, guis.confirmDisband(player));
                } else {
                    teams.leave(player);
                }
            }
            default -> {
            }
        }
    }

    private Component infoLine(Team team, Player player) {
        return messages.component("info.header", "<gold><team>", "team", team.name(), "tag", team.tag());
    }

    static final class ItemStackFiller {
        private ItemStackFiller() {
        }

        static void fill(Inventory inventory) {
            var filler = ItemBuilder.filler();
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }
    }
}
