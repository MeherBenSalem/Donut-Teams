package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.service.ChatService;
import io.nightbeam.donutteams.service.HomeWarmupService;
import io.nightbeam.donutteams.service.TeamService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GuiManager implements Listener {

    private final DonutTeamsPlugin plugin;
    private final Messages messages;
    private final FoliaScheduler scheduler;
    private final TeamService teams;
    private final ChatService chat;
    private final HomeWarmupService homes;
    private final Map<UUID, Long> disbandConfirm = new ConcurrentHashMap<>();

    public GuiManager(
            DonutTeamsPlugin plugin,
            Messages messages,
            FoliaScheduler scheduler,
            TeamService teams,
            ChatService chat,
            HomeWarmupService homes
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.scheduler = scheduler;
        this.teams = teams;
        this.chat = chat;
        this.homes = homes;
    }

    public void openMain(Player player) {
        open(player, new TeamMainGui(this, messages, teams, chat));
    }

    public void openMembers(Player player) {
        open(player, new MembersGui(this, messages, teams));
    }

    public void openMember(Player player, UUID targetId) {
        open(player, new MemberEditGui(this, messages, teams, targetId));
    }

    public void openInvites(Player player) {
        open(player, new InvitesGui(this, messages, teams));
    }

    public void openSettings(Player player) {
        open(player, new SettingsGui(this, messages, teams));
    }

    public void open(Player player, BaseGui gui) {
        scheduler.runForEntity(player, () -> player.openInventory(gui.render(player)));
    }

    public TeamService teams() {
        return teams;
    }

    public ChatService chat() {
        return chat;
    }

    public HomeWarmupService homes() {
        return homes;
    }

    public DonutTeamsPlugin plugin() {
        return plugin;
    }

    public boolean confirmDisband(Player player) {
        long now = System.currentTimeMillis();
        Long last = disbandConfirm.get(player.getUniqueId());
        if (last != null && now - last < 8_000L) {
            disbandConfirm.remove(player.getUniqueId());
            return true;
        }
        disbandConfirm.put(player.getUniqueId(), now);
        return false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        gui.handleClick(player, event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGui) {
            event.setCancelled(true);
        }
    }
}
