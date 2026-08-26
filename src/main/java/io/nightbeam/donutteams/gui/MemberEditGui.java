package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class MemberEditGui extends BaseGui {

    private static final TeamPermission[] ORDER = {
            TeamPermission.INVITE,
            TeamPermission.KICK,
            TeamPermission.HOME,
            TeamPermission.SETHOME,
            TeamPermission.SPEAK,
            TeamPermission.PVP
    };

    private final GuiManager guis;
    private final Messages messages;
    private final TeamService teams;
    private final UUID targetId;

    public MemberEditGui(GuiManager guis, Messages messages, TeamService teams, UUID targetId) {
        this.guis = guis;
        this.messages = messages;
        this.teams = teams;
        this.targetId = targetId;
    }

    @Override
    public Inventory render(Player player) {
        Team team = teams.teamByPlayer(player.getUniqueId());
        TeamMember target = team == null ? null : team.member(targetId);
        String name = target == null ? "?" : target.name();
        Inventory inventory = attach(Bukkit.createInventory(this, 27,
                messages.component("gui.title-member", "<gold>Member", "player", name)));
        TeamMainGui.ItemStackFiller.fill(inventory);
        if (target == null) {
            return inventory;
        }
        for (int i = 0; i < ORDER.length; i++) {
            TeamPermission permission = ORDER[i];
            boolean on = target.has(permission);
            inventory.setItem(10 + i, ItemBuilder.of(
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    messages.component("gui.toggle-perm", "<yellow>Toggle", "perm", permission.name()),
                    List.of(messages.component(on ? "gui.pvp-on" : "gui.pvp-off", on ? "<green>ON" : "<red>OFF"))));
        }
        inventory.setItem(21, ItemBuilder.of(Material.IRON_AXE, messages.component("gui.kick-name", "<red>Kick"), List.of()));
        inventory.setItem(23, ItemBuilder.of(Material.GOLDEN_HELMET, messages.component("gui.transfer-name", "<gold>Transfer ownership"), List.of()));
        inventory.setItem(22, ItemBuilder.of(Material.ARROW, messages.component("gui.back-name", "<gray>Back"), List.of()));
        return inventory;
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 22) {
            guis.openMembers(player);
            return;
        }
        Team team = teams.teamByPlayer(player.getUniqueId());
        TeamMember target = team == null ? null : team.member(targetId);
        if (target == null) {
            return;
        }
        if (slot >= 10 && slot <= 15) {
            teams.togglePermission(player, targetId, ORDER[slot - 10]);
            return;
        }
        if (slot == 21) {
            player.closeInventory();
            teams.kick(player, target.name());
            return;
        }
        if (slot == 23) {
            player.closeInventory();
            teams.transfer(player, target.name());
        }
    }
}
