package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.service.Format;
import org.bukkit.entity.Player;

/** Yes or no before something that cannot be undone. */
final class ConfirmMenu extends Menu {

    private final String question;
    private final String detail;
    private final Runnable confirm;
    private final Runnable cancel;
    private final Object[] values;
    private boolean answered;

    ConfirmMenu(DonutTeamsPlugin plugin, Player viewer, String question, String detail, Runnable confirm, Runnable cancel,
                Object... values) {
        super(plugin, viewer, "confirm");
        this.question = question;
        this.detail = detail;
        this.confirm = confirm;
        this.cancel = cancel;
        this.values = values;
    }

    @Override
    protected Object[] placeholders() {
        // The question and detail are templates themselves, filled with the same values.
        return Format.concat(values, "question", messages().render(question, values), "detail", messages().render(detail, values));
    }

    @Override
    protected void build() {
        place("about", null);
        place("confirm", () -> {
            if (answered) {
                return;
            }
            answered = true;
            close();
            confirm.run();
        });
        place("cancel", () -> {
            if (answered) {
                return;
            }
            answered = true;
            if (cancel != null) {
                cancel.run();
            } else {
                close();
            }
        });
    }
}
