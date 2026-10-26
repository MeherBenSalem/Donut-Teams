package io.nightbeam.teamlab;

import io.nightbeam.donutteams.api.event.TeamBankTransactionEvent;
import io.nightbeam.donutteams.api.event.TeamChatEvent;
import io.nightbeam.donutteams.api.event.TeamCreateEvent;
import io.nightbeam.donutteams.api.event.TeamDisbandEvent;
import io.nightbeam.donutteams.api.event.TeamHomeTeleportEvent;
import io.nightbeam.donutteams.api.event.TeamJoinEvent;
import io.nightbeam.donutteams.api.event.TeamLeaveEvent;
import io.nightbeam.donutteams.api.event.TeamLevelUpEvent;
import io.nightbeam.donutteams.api.event.TeamRelationChangeEvent;
import io.nightbeam.donutteams.api.event.TeamRenameEvent;
import io.nightbeam.donutteams.api.event.TeamRoleChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Prints Donut Teams' API events so the runner can see them. */
final class ApiListener implements Listener {

    private final TeamLabPlugin plugin;

    ApiListener(TeamLabPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(TeamCreateEvent event) {
        plugin.report("event create " + event.getPlayer().getName() + " " + event.getTeamName() + " " + event.getTeamTag());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(TeamJoinEvent event) {
        plugin.report("event join " + event.getPlayer().getName() + " " + event.getTeamName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeave(TeamLeaveEvent event) {
        plugin.report("event leave " + event.getPlayerId() + " " + event.getTeamName() + " " + event.getReason());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(TeamDisbandEvent event) {
        plugin.report("event disband " + event.getTeamName() + " admin=" + event.isAdmin());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRole(TeamRoleChangeEvent event) {
        plugin.report("event role " + event.getTeamName() + " " + event.getFrom() + ">" + event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRelation(TeamRelationChangeEvent event) {
        plugin.report("event relation " + event.getTeamName() + " " + event.getOtherTeamName() + " " + event.getFrom() + ">" + event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBank(TeamBankTransactionEvent event) {
        plugin.report("event bank " + event.getTeamName() + " " + event.getType() + " " + event.getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHome(TeamHomeTeleportEvent event) {
        plugin.report("event home " + event.getPlayer().getName() + " " + event.getTeamName() + " " + event.getHomeName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevel(TeamLevelUpEvent event) {
        plugin.report("event level " + event.getTeamName() + " " + event.getFrom() + ">" + event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRename(TeamRenameEvent event) {
        plugin.report("event rename " + event.getTeamName() + ">" + event.getNewName() + " " + event.getNewTag());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(TeamChatEvent event) {
        plugin.report("event chat " + event.getPlayer().getName() + " ally=" + event.isAllyChat() + " " + event.getMessage());
    }
}
