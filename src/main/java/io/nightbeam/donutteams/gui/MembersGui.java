package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class MembersGui extends BaseGui {

    private final GuiManager guis;
    private final Messages messages;
    private final TeamService teams;

    public MembersGui(GuiManager guis, Messages messages, TeamService teams) {
        this.guis = guis;
        this.messages = messages;
        this.teams = teams;
    }

    @Override
    public Inventory render(Player player) {
        Inventory inventory = attach(Bukkit.createInventory(this, 54, messages.component("gui.title-members", "<gold>Team Members")));
        TeamMainGui.ItemStackFiller.fill(inventory);
        Team team = teams.teamByPlayer(player.getUniqueId());
        if (team == null) {
            return inventory;
        }
        int slot = 10;
        for (TeamMember member : team.members()) {
            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            if (slot >= 44) {
                break;
            }
            String badge = member.isOwner()
                    ? messages.raw("gui.owner-badge", "<gold>Owner")
                    : messages.raw("gui.member-badge", "<gray>Member");
            inventory.setItem(slot, ItemBuilder.skull(
                    member.playerId(),
                    messages.deserialize("<white>" + member.name()),
                    List.of(messages.deserialize(badge))));
            slot++;
        }
        inventory.setItem(49, ItemBuilder.of(Material.ARROW, messages.component("gui.back-name", "<gray>Back"), List.of()));
        return inventory;
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        if (event.getRawSlot() == 49) {
            guis.openMain(player);
            return;
        }
        Team team = teams.teamByPlayer(player.getUniqueId());
        if (team == null || !team.ownerId().equals(player.getUniqueId())) {
            return;
        }
        if (!(event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD)) {
            return;
        }
        int index = 0;
        int slot = 10;
        for (TeamMember member : team.members()) {
            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            if (slot == event.getRawSlot()) {
                guis.openMember(player, member.playerId());
                return;
            }
            slot++;
            index++;
        }
    }
}
