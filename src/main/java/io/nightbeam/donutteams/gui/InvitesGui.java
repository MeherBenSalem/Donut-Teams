package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamInvite;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class InvitesGui extends BaseGui {

    private final GuiManager guis;
    private final Messages messages;
    private final TeamService teams;

    public InvitesGui(GuiManager guis, Messages messages, TeamService teams) {
        this.guis = guis;
        this.messages = messages;
        this.teams = teams;
    }

    @Override
    public Inventory render(Player player) {
        Inventory inventory = attach(Bukkit.createInventory(this, 27, messages.component("gui.title-invites", "<gold>Team Invites")));
        TeamMainGui.ItemStackFiller.fill(inventory);
        Team team = teams.teamByPlayer(player.getUniqueId());
        int slot = 10;
        if (team != null) {
            for (TeamInvite invite : teams.cache().teamInvites(team.id())) {
                if (slot >= 16) {
                    break;
                }
                String name = Bukkit.getOfflinePlayer(invite.playerId()).getName();
                inventory.setItem(slot, ItemBuilder.skull(
                        invite.playerId(),
                        messages.deserialize("<white>" + (name == null ? invite.playerId().toString() : name)),
                        List.of(messages.deserialize("<gray>Pending invite"))));
                slot++;
            }
        }
        inventory.setItem(22, ItemBuilder.of(Material.ARROW, messages.component("gui.back-name", "<gray>Back"), List.of()));
        return inventory;
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        if (event.getRawSlot() == 22) {
            guis.openMain(player);
        }
    }
}
