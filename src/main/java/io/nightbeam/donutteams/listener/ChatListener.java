package io.nightbeam.donutteams.listener;

import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.service.ChatService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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
        boolean pending = chat.hasPending(player.getUniqueId());
        boolean toggled = chat.toggled(player.getUniqueId());
        if (!pending && !toggled) {
            return;
        }
        event.setCancelled(true);
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        scheduler.runForEntity(player, () -> {
            if (chat.handleInput(player, plain)) {
                return;
            }
            chat.handleToggledChat(player, event.message());
        });
    }
}
