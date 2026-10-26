package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.storage.DataWriter;
import io.nightbeam.donutteams.util.ItemCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The team chest: up to six rows, set by the team level. Contents are always kept as 54 slots, so lowering a level
 * hides rows instead of deleting items. On Paper several members can use it at once (one shared inventory); on
 * Folia, where each player may tick on a different thread, one member at a time.
 */
public final class ChestService {

    public static final int STORED_SLOTS = 54;

    /** The inventory holder of an open team chest. */
    public static final class Holder implements InventoryHolder {
        private final UUID teamId;
        private Inventory inventory;

        Holder(UUID teamId) {
            this.teamId = teamId;
        }

        public UUID teamId() {
            return teamId;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private static final class Chest {
        final UUID teamId;
        volatile ItemStack[] contents = new ItemStack[STORED_SLOTS];
        volatile Holder holder;
        final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        volatile boolean dirty;

        Chest(UUID teamId) {
            this.teamId = teamId;
        }
    }

    private final DonutTeamsPlugin plugin;
    private final Supplier<Settings> settings;
    private final Messages messages;
    private final FoliaScheduler scheduler;
    private final TeamService teams;
    private final DataWriter writer;
    private final Map<UUID, Chest> chests = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Chest>> loading = new ConcurrentHashMap<>();

    public ChestService(DonutTeamsPlugin plugin, Supplier<Settings> settings, Messages messages, FoliaScheduler scheduler,
                        TeamService teams, DataWriter writer) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.scheduler = scheduler;
        this.teams = teams;
        this.writer = writer;
    }

    public void open(Player player) {
        if (!settings.get().chestEnabled) {
            messages.send(player, "error.feature-disabled");
            return;
        }
        Team team = teams.requireTeam(player);
        if (team == null || !teams.requirePermission(player, team, TeamPermission.CHEST)) {
            return;
        }
        open(player, team, false);
    }

    /** Opens a team's chest; admins may open any team's. */
    public void open(Player player, Team team, boolean admin) {
        load(team.id()).whenComplete((chest, error) -> scheduler.runForEntity(player, () -> {
            if (error != null) {
                plugin.getLogger().warning("Could not load the chest of " + team.name() + ": " + error);
                messages.send(player, "error.generic");
                return;
            }
            if (teams.teamById(team.id()) == null) {
                return;
            }
            if (scheduler.isFolia() && !chest.viewers.isEmpty() && !chest.viewers.contains(player.getUniqueId())) {
                Player viewer = Bukkit.getPlayer(chest.viewers.iterator().next());
                messages.send(player, "chest.in-use", "player", viewer == null ? "?" : viewer.getName());
                return;
            }
            Holder holder = chest.holder;
            int rows = teams.format().chestRows(team);
            if (holder == null || holder.inventory.getSize() != rows * 9) {
                if (holder != null && !chest.viewers.isEmpty()) {
                    // Rows changed while someone has it open: keep the open size until everyone closes it.
                    rows = holder.inventory.getSize() / 9;
                } else {
                    holder = new Holder(team.id());
                    holder.inventory = Bukkit.createInventory(holder, rows * 9, messages.component("chest.title",
                            teams.format().team(team)));
                    ItemStack[] visible = new ItemStack[rows * 9];
                    System.arraycopy(chest.contents, 0, visible, 0, visible.length);
                    holder.inventory.setContents(visible);
                    chest.holder = holder;
                }
            }
            chest.viewers.add(player.getUniqueId());
            player.openInventory(chest.holder.inventory);
            plugin.sounds().play(player, "chest-open");
            if (admin) {
                messages.send(player, "admin.chest", teams.format().team(team));
            }
        }));
    }

    /** Called when a team chest closes: the last viewer out saves it. */
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Holder holder)) {
            return;
        }
        Chest chest = chests.get(holder.teamId);
        if (chest == null) {
            return;
        }
        chest.viewers.remove(event.getPlayer().getUniqueId());
        capture(chest, holder.inventory);
        if (chest.viewers.isEmpty()) {
            save(chest);
            chest.holder = null;
        }
        if (event.getPlayer() instanceof Player player) {
            plugin.sounds().play(player, "chest-close");
        }
    }

    public void markDirty(Inventory inventory) {
        if (inventory.getHolder(false) instanceof Holder holder) {
            Chest chest = chests.get(holder.teamId);
            if (chest != null) {
                chest.dirty = true;
            }
        }
    }

    public boolean inUse(UUID teamId) {
        Chest chest = chests.get(teamId);
        return chest != null && !chest.viewers.isEmpty();
    }

    /** Autosave: each open chest is read on one viewer's thread and saved. */
    public void autosave() {
        for (Chest chest : chests.values()) {
            Holder holder = chest.holder;
            if (holder == null || chest.viewers.isEmpty()) {
                continue;
            }
            Player viewer = Bukkit.getPlayer(chest.viewers.iterator().next());
            if (viewer == null) {
                continue;
            }
            scheduler.runForEntity(viewer, () -> {
                capture(chest, holder.inventory);
                save(chest);
            });
        }
    }

    /** After a level change: a closed chest is rebuilt with the new size the next time it opens. */
    public void resized(Team team) {
        Chest chest = chests.get(team.id());
        if (chest != null && chest.viewers.isEmpty()) {
            chest.holder = null;
        }
    }

    /** Removes a disbanded team's chest and returns what was in it. Fails while someone has it open. */
    public CompletableFuture<List<ItemStack>> takeContents(UUID teamId) {
        Chest loaded = chests.remove(teamId);
        if (loaded != null) {
            return CompletableFuture.completedFuture(nonEmpty(loaded.contents));
        }
        return writer.query(repository -> repository.loadChest(teamId))
                .thenApply(data -> nonEmpty(ItemCodec.decodeAll(data, STORED_SLOTS)));
    }

    /** Saves every chest now (plugin disable). Inventories are read on this thread; the server is stopping. */
    public void saveAll() {
        for (Chest chest : chests.values()) {
            Holder holder = chest.holder;
            if (holder != null) {
                for (var viewer : List.copyOf(holder.inventory.getViewers())) {
                    viewer.closeInventory();
                }
                capture(chest, holder.inventory);
            }
            if (chest.dirty) {
                save(chest);
            }
        }
    }

    private CompletableFuture<Chest> load(UUID teamId) {
        Chest existing = chests.get(teamId);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        // Not computeIfAbsent: the query can finish before it returns, and the callback must not change the map then.
        CompletableFuture<Chest> created = new CompletableFuture<>();
        CompletableFuture<Chest> pending = loading.putIfAbsent(teamId, created);
        if (pending != null) {
            return pending;
        }
        writer.query(repository -> repository.loadChest(teamId)).whenComplete((data, error) -> {
            loading.remove(teamId, created);
            if (error != null) {
                created.completeExceptionally(error);
                return;
            }
            Chest chest = new Chest(teamId);
            chest.contents = ItemCodec.decodeAll(data, STORED_SLOTS);
            Chest raced = chests.putIfAbsent(teamId, chest);
            created.complete(raced == null ? chest : raced);
        });
        return created;
    }

    private static void capture(Chest chest, Inventory inventory) {
        ItemStack[] visible = inventory.getContents();
        ItemStack[] next = chest.contents.clone();
        for (int slot = 0; slot < visible.length && slot < next.length; slot++) {
            next[slot] = visible[slot] == null ? null : visible[slot].clone();
        }
        chest.contents = next;
        chest.dirty = true;
    }

    private void save(Chest chest) {
        if (!chest.dirty) {
            return;
        }
        chest.dirty = false;
        String data = ItemCodec.encodeAll(chest.contents);
        writer.write("team chest", repository -> repository.saveChest(chest.teamId, data));
    }

    private static List<ItemStack> nonEmpty(ItemStack[] items) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                out.add(item);
            }
        }
        return out;
    }
}
