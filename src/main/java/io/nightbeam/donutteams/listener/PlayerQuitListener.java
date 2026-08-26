package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.service.ChatService;
import io.nightbeam.donutteams.service.HomeWarmupService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final ChatService chat;
    private final HomeWarmupService homes;

    public PlayerQuitListener(ChatService chat, HomeWarmupService homes) {
        this.chat = chat;
        this.homes = homes;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chat.clear(event.getPlayer().getUniqueId());
        homes.clear(event.getPlayer().getUniqueId());
    }
}
