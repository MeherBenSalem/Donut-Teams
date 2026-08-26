package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.service.TeamService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {

    private final FoliaScheduler scheduler;
    private final TeamService teams;

    public PlayerJoinListener(FoliaScheduler scheduler, TeamService teams) {
        this.scheduler = scheduler;
        this.teams = teams;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduler.runForEntity(event.getPlayer(), () -> teams.updateName(event.getPlayer()));
    }
}
