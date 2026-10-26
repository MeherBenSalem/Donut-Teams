package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.service.ChatService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Team chat, ally chat and prompt answers are taken out of public chat and handled on the player's thread. */
public final class ChatListener implements Listener {

    private final ChatService chat;
    private final FoliaScheduler scheduler;

    public ChatListener(ChatService chat, FoliaScheduler scheduler) {
        this.chat = chat;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (!chat.wants(player, plain)) {
            return;
        }
        event.setCancelled(true);
        scheduler.runForEntity(player, () -> chat.handle(player, plain));
    }
}
