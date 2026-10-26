package io.nightbeam.donutteams.dialog;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.service.InputService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

/**
 * Native input dialogs (Minecraft 1.21.7+): a text box instead of typing in chat. Compiled against the 1.21.11 API
 * and loaded by {@code DialogSupport} only when the server has Paper's dialog API.
 */
public final class PaperDialogs implements InputService.Dialogs {

    private static final ClickCallback.Options ONCE = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build();

    private final DonutTeamsPlugin plugin;

    public PaperDialogs(DonutTeamsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void text(Player player, String title, String label, String initial, Consumer<String> answer, Runnable cancelled) {
        Messages messages = plugin.messages();
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(messages.render(title))
                        .canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(messages.render(label), 300)))
                        .inputs(List.of(DialogInput.text("value", messages.render(messages.raw("dialog.text-field")))
                                .initial(initial == null ? "" : initial)
                                .maxLength(160)
                                .width(300)
                                .build()))
                        .build())
                .type(DialogType.confirmation(
                        button(messages.raw("dialog.submit"), DialogAction.customClick((view, audience) -> {
                            String value = view.getText("value");
                            if (value == null || value.isBlank()) {
                                cancelled.run();
                            } else {
                                answer.accept(value);
                            }
                        }, ONCE)),
                        button(messages.raw("dialog.cancel"), DialogAction.customClick((view, audience) -> cancelled.run(), ONCE)))));
        player.showDialog(dialog);
    }

    @Override
    public void create(Player player, Consumer<String[]> answer) {
        Messages messages = plugin.messages();
        Settings settings = plugin.settings();
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(messages.render(messages.raw("dialog.create-title")))
                        .canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(messages.component("prompt.create"), 300)))
                        .inputs(List.of(
                                DialogInput.text("name", messages.render(messages.raw("dialog.create-name")))
                                        .maxLength(settings.nameMax).width(260).build(),
                                DialogInput.text("tag", messages.render(messages.raw("dialog.create-tag")))
                                        .maxLength(settings.tagMax).width(160).build()))
                        .build())
                .type(DialogType.confirmation(
                        button(messages.raw("dialog.confirm"), DialogAction.customClick((view, audience) -> {
                            String name = view.getText("name");
                            String tag = view.getText("tag");
                            if (name == null || name.isBlank()) {
                                return;
                            }
                            answer.accept(new String[] {name.trim(), tag == null ? "" : tag.trim()});
                        }, ONCE)),
                        button(messages.raw("dialog.cancel"), null))));
        player.showDialog(dialog);
    }

    private ActionButton button(String label, DialogAction action) {
        Component text = plugin.messages().render(label);
        return ActionButton.create(text, null, 120, action);
    }
}
