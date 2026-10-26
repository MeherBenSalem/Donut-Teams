package io.nightbeam.donutteams;

import io.nightbeam.donutteams.service.InputService;

/**
 * Native input dialogs exist from Minecraft 1.21.7 (Paper's dialog API). The implementation is compiled separately
 * against a newer API and only loaded when the running server has it, so older servers never see those classes.
 */
final class DialogSupport {

    private static final String IMPLEMENTATION = "io.nightbeam.donutteams.dialog.PaperDialogs";

    private DialogSupport() {
    }

    static InputService.Dialogs create(DonutTeamsPlugin plugin) {
        try {
            Class.forName("io.papermc.paper.dialog.Dialog");
        } catch (ClassNotFoundException ignored) {
            return null;
        }
        try {
            Class<?> type = Class.forName(IMPLEMENTATION);
            return (InputService.Dialogs) type.getConstructor(DonutTeamsPlugin.class).newInstance(plugin);
        } catch (Throwable error) {
            plugin.getLogger().fine("Dialogs are not available: " + error);
            return null;
        }
    }
}
