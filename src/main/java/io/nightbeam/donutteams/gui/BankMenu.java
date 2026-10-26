package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.BankEntry;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.service.Format;
import io.nightbeam.donutteams.util.Amounts;
import io.nightbeam.donutteams.util.Durations;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Deposit and withdraw with preset or typed amounts; the latest moves on the history book. */
final class BankMenu extends Menu {

    private List<BankEntry> history;

    BankMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "bank");
    }

    @Override
    protected Object[] placeholders() {
        Team team = team();
        if (team == null) {
            return new Object[0];
        }
        return format().team(team, "wallet", format().money(plugin.economy().balance(viewer)));
    }

    @Override
    public void open() {
        super.open();
        Team team = team();
        if (team != null && history == null) {
            plugin.bank().history(team).whenComplete((entries, error) -> {
                history = error == null ? entries : List.of();
                redraw();
            });
        }
    }

    @Override
    protected void build() {
        Team team = team();
        if (team == null) {
            new NoTeamMenu(plugin, viewer).open();
            return;
        }
        place("balance", null);
        for (String key : List.of("deposit-1", "deposit-2", "deposit-3", "withdraw-1", "withdraw-2", "withdraw-3")) {
            MenuConfig.Item spec = item(key);
            if (spec == null) {
                continue;
            }
            double amount = spec.number("amount", 0);
            boolean deposit = key.startsWith("deposit");
            place(key, null, (type, slot) -> {
                if (deposit) {
                    plugin.bank().deposit(viewer, amount);
                } else {
                    plugin.bank().withdraw(viewer, amount);
                }
                reopen();
            }, "amount", format().money(amount));
        }
        place("deposit-custom", () -> plugin.input().text(viewer, "prompt.deposit", answer -> {
            plugin.bank().deposit(viewer, answer);
            reopen();
        }));
        place("withdraw-custom", () -> plugin.input().text(viewer, "prompt.withdraw", answer -> {
            plugin.bank().withdraw(viewer, answer);
            reopen();
        }));
        MenuConfig.Item spec = item("history");
        if (spec != null) {
            List<Component> lore = new ArrayList<>();
            for (String line : spec.list("lore-header")) {
                lore.add(messages().render(line));
            }
            if (history == null) {
                lore.add(messages().render(spec.string("loading", "...")));
            } else if (history.isEmpty()) {
                for (String line : spec.list("lore-empty")) {
                    lore.add(messages().render(line));
                }
            } else {
                long now = System.currentTimeMillis();
                for (BankEntry entry : history.subList(0, Math.min(10, history.size()))) {
                    String template = spec.string("entry-" + entry.type().name().toLowerCase(Locale.ROOT), "<gray><amount>");
                    lore.add(messages().render(template, "amount", Amounts.shortForm(entry.amount()), "player", entry.playerName(),
                            "ago", Durations.ago(entry.atMillis(), now)));
                }
            }
            ItemStack book = ItemBuilder.decorate(new ItemStack(material(spec.material()) == Material.STONE ? Material.BOOK
                    : material(spec.material())), messages().render(spec.name(), placeholders()), lore, false);
            for (int slot : spec.slots()) {
                set(slot, book, null);
            }
        }
        back(() -> new MainMenu(plugin, viewer).open());
    }

    private void reopen() {
        plugin.scheduler().runLaterForEntity(viewer, () -> {
            BankMenu next = new BankMenu(plugin, viewer);
            next.open();
        }, 2L);
    }

    static Object[] values(Object[] base, Object... extra) {
        return Format.concat(base, extra);
    }
}
