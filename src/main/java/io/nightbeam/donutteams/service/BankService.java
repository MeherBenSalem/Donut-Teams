package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.api.event.TeamBankTransactionEvent;
import io.nightbeam.donutteams.api.event.TeamLevelUpEvent;
import io.nightbeam.donutteams.config.LevelTable;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.hook.Economy;
import io.nightbeam.donutteams.model.BankEntry;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.storage.DataWriter;
import io.nightbeam.donutteams.util.Amounts;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Team banks (through Vault) and levels. Player money is moved first; if the team side then fails, the player is
 * refunded, so money is never created or lost.
 */
public final class BankService {

    private final DonutTeamsPlugin plugin;
    private final Supplier<Settings> settings;
    private final Messages messages;
    private final TeamService teams;
    private final Economy economy;
    private final DataWriter writer;
    private final ReentrantLock lock = new ReentrantLock();

    public BankService(DonutTeamsPlugin plugin, Supplier<Settings> settings, Messages messages, TeamService teams, Economy economy,
                       DataWriter writer) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.teams = teams;
        this.economy = economy;
        this.writer = writer;
    }

    public boolean enabled() {
        return settings.get().bankEnabled && economy.available();
    }

    public void balance(Player player) {
        Team team = teams.requireTeam(player);
        if (team == null || !requireBank(player)) {
            return;
        }
        messages.send(player, "bank.balance", teams.format().team(team));
    }

    public void deposit(Player player, String rawAmount) {
        double amount = Amounts.parse(rawAmount);
        if (amount <= 0) {
            messages.send(player, "error.invalid-amount", "input", rawAmount);
            return;
        }
        deposit(player, amount);
    }

    public void deposit(Player player, double amount) {
        Team team = teams.requireTeam(player);
        if (team == null || !requireBank(player) || !teams.requirePermission(player, team, TeamPermission.BANK_DEPOSIT)) {
            return;
        }
        double max = settings.get().bankMaxBalance;
        if (max > 0 && team.balance() + amount > max) {
            messages.send(player, "bank.max-balance", "max", teams.format().money(max));
            return;
        }
        if (!economy.has(player, amount)) {
            messages.send(player, "bank.cannot-afford", "amount", teams.format().money(amount));
            plugin.sounds().play(player, "error");
            return;
        }
        if (!TeamService.call(new TeamBankTransactionEvent(team.id(), team.name(), player, BankEntry.Type.DEPOSIT, amount))) {
            messages.send(player, "error.cancelled");
            return;
        }
        if (!economy.withdraw(player, amount)) {
            messages.send(player, "bank.cannot-afford", "amount", teams.format().money(amount));
            return;
        }
        double after;
        lock.lock();
        try {
            team.balance(team.balance() + amount);
            after = team.balance();
        } finally {
            lock.unlock();
        }
        record(team, player, BankEntry.Type.DEPOSIT, amount, after);
        teams.broadcast(team, "bank.deposited", teams.format().team(team, "player", teams.display(player),
                "amount", teams.format().money(amount)));
        plugin.sounds().play(player, "bank");
    }

    public void withdraw(Player player, String rawAmount) {
        double amount = Amounts.parse(rawAmount);
        if (amount <= 0) {
            messages.send(player, "error.invalid-amount", "input", rawAmount);
            return;
        }
        withdraw(player, amount);
    }

    public void withdraw(Player player, double amount) {
        Team team = teams.requireTeam(player);
        if (team == null || !requireBank(player) || !teams.requirePermission(player, team, TeamPermission.BANK_WITHDRAW)) {
            return;
        }
        if (!TeamService.call(new TeamBankTransactionEvent(team.id(), team.name(), player, BankEntry.Type.WITHDRAW, amount))) {
            messages.send(player, "error.cancelled");
            return;
        }
        double after;
        lock.lock();
        try {
            if (team.balance() < amount) {
                messages.send(player, "bank.team-cannot-afford", teams.format().team(team, "amount", teams.format().money(amount)));
                return;
            }
            team.balance(team.balance() - amount);
            after = team.balance();
        } finally {
            lock.unlock();
        }
        if (!economy.deposit(player, amount)) {
            lock.lock();
            try {
                team.balance(team.balance() + amount);
            } finally {
                lock.unlock();
            }
            messages.send(player, "error.generic");
            return;
        }
        record(team, player, BankEntry.Type.WITHDRAW, amount, after);
        teams.broadcast(team, "bank.withdrew", teams.format().team(team, "player", teams.display(player),
                "amount", teams.format().money(amount)));
        plugin.sounds().play(player, "bank");
    }

    /** Buys the next level with team bank money. */
    public void levelUp(Player player) {
        Settings s = settings.get();
        Team team = teams.requireTeam(player);
        if (team == null) {
            return;
        }
        if (!s.levelsEnabled) {
            messages.send(player, "error.feature-disabled");
            return;
        }
        if (!teams.requirePermission(player, team, TeamPermission.LEVEL_UP)) {
            return;
        }
        LevelTable.Level next = s.levels.next(team.level());
        if (next == null) {
            messages.send(player, "level.max", teams.format().team(team));
            return;
        }
        if (next.cost() > 0 && !requireBank(player)) {
            return;
        }
        if (team.balance() < next.cost()) {
            messages.send(player, "level.cannot-afford", teams.format().team(team, "cost", teams.format().money(next.cost())));
            plugin.sounds().play(player, "error");
            return;
        }
        if (!TeamService.call(new TeamLevelUpEvent(team.id(), team.name(), team.level(), next.level(), next.cost()))
                || (next.cost() > 0 && !TeamService.call(new TeamBankTransactionEvent(team.id(), team.name(), player,
                BankEntry.Type.LEVEL_UP, next.cost())))) {
            messages.send(player, "error.cancelled");
            return;
        }
        double after;
        lock.lock();
        try {
            if (team.balance() < next.cost() || team.level() + 1 != next.level()) {
                messages.send(player, "level.cannot-afford", teams.format().team(team, "cost", teams.format().money(next.cost())));
                return;
            }
            team.balance(team.balance() - next.cost());
            team.level(next.level());
            after = team.balance();
        } finally {
            lock.unlock();
        }
        if (next.cost() > 0) {
            record(team, player, BankEntry.Type.LEVEL_UP, next.cost(), after);
        } else {
            teams.saveTeam(team);
        }
        plugin.chests().resized(team);
        teams.broadcast(team, "level.upgraded", teams.format().team(team, "player", teams.display(player)));
        teams.broadcast(team, "level.perks", teams.format().team(team));
        plugin.sounds().playTeam(team, "level-up");
    }

    /** Admin: set, give or take team money (no player money moves). */
    public void adminBalance(CommandSender actor, Team team, String action, double amount) {
        double after;
        lock.lock();
        try {
            switch (action) {
                case "set" -> team.balance(amount);
                case "give" -> team.balance(team.balance() + amount);
                case "take" -> team.balance(Math.max(0, team.balance() - amount));
                default -> {
                    return;
                }
            }
            after = team.balance();
        } finally {
            lock.unlock();
        }
        UUID actorId = actor instanceof Player player ? player.getUniqueId() : null;
        BankEntry entry = new BankEntry(team.id(), actorId, actor.getName(), BankEntry.Type.ADMIN, amount, after, System.currentTimeMillis());
        int keep = settings.get().bankHistorySize;
        writer.write("bank", repository -> {
            repository.saveTeam(team);
            repository.insertBankEntry(entry, keep);
        });
        messages.send(actor, "admin.balance", teams.format().team(team));
    }

    public void adminLevel(CommandSender actor, Team team, int level) {
        int target = Math.max(1, Math.min(level, settings.get().levels.max()));
        if (!TeamService.call(new TeamLevelUpEvent(team.id(), team.name(), team.level(), target, 0))) {
            messages.send(actor, "error.cancelled");
            return;
        }
        team.level(target);
        teams.saveTeam(team);
        plugin.chests().resized(team);
        messages.send(actor, "admin.level", teams.format().team(team));
        teams.broadcast(team, "level.perks", teams.format().team(team));
    }

    public CompletableFuture<List<BankEntry>> history(Team team) {
        return writer.query(repository -> repository.loadBankHistory(team.id(), Math.max(1, settings.get().bankHistorySize)));
    }

    private void record(Team team, Player player, BankEntry.Type type, double amount, double after) {
        BankEntry entry = new BankEntry(team.id(), player.getUniqueId(), player.getName(), type, amount, after, System.currentTimeMillis());
        int keep = settings.get().bankHistorySize;
        writer.write("bank", repository -> {
            repository.saveTeam(team);
            repository.insertBankEntry(entry, keep);
        });
    }

    private boolean requireBank(Player player) {
        if (!settings.get().bankEnabled) {
            messages.send(player, "error.feature-disabled");
            return false;
        }
        if (!economy.available()) {
            messages.send(player, "bank.no-economy");
            return false;
        }
        return true;
    }
}
