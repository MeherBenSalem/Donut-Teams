package io.nightbeam.teamlab;

import io.nightbeam.donutteams.api.DonutTeams;
import io.nightbeam.donutteams.api.DonutTeamsApi;
import io.nightbeam.donutteams.api.TeamSnapshot;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Console helpers for the Donut Teams smoke matrix. Every command prints one {@code [TEAMLAB] <command> ...} line
 * (or {@code [TEAMLAB] error ...}) that the runner waits for. Player work runs on the player's own thread (Folia).
 */
public final class TeamLabPlugin extends JavaPlugin {

    private final LabEconomy economy = new LabEconomy();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null || Bukkit.getPluginManager().getPlugin("VaultUnlocked") != null) {
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, economy, this, ServicePriority.Highest);
        }
        Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> {
            var teams = Bukkit.getPluginManager().getPlugin("DonutTeams");
            if (teams != null && teams.isEnabled() && teams.getPluginMeta().getVersion().startsWith("2")) {
                Bukkit.getPluginManager().registerEvents(new ApiListener(this), this);
                report("api listening");
            } else {
                report("api absent");
            }
        }, 1L);
    }

    void report(String line) {
        getLogger().info("[TEAMLAB] " + line);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "money" -> {
                    Player player = player(args[1]);
                    economy.set(player.getUniqueId(), Double.parseDouble(args[2]));
                    report("money " + player.getName() + " " + economy.balance(player.getUniqueId()));
                }
                case "balance" -> {
                    Player player = player(args[1]);
                    report("balance " + player.getName() + " " + economy.balance(player.getUniqueId()));
                }
                case "perm" -> {
                    Player player = player(args[1]);
                    boolean value = Boolean.parseBoolean(args[3]);
                    player.getScheduler().run(this, task -> {
                        player.addAttachment(this, args[2], value);
                        player.recalculatePermissions();
                        report("perm " + player.getName() + " " + args[2] + "=" + player.hasPermission(args[2]));
                    }, () -> report("error perm retired"));
                }
                case "pos" -> {
                    Player player = player(args[1]);
                    player.getScheduler().run(this, task -> {
                        Location at = player.getLocation();
                        report(String.format(Locale.ROOT, "pos %s %s %.2f %.2f %.2f", player.getName(), at.getWorld().getName(),
                                at.getX(), at.getY(), at.getZ()));
                    }, () -> report("error pos retired"));
                }
                case "tp" -> {
                    Player player = player(args[1]);
                    World world = world(args[2]);
                    Location to = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]));
                    player.getScheduler().run(this, task -> player.teleportAsync(to)
                            .thenAccept(ok -> report("tp " + player.getName() + " " + ok)), () -> report("error tp retired"));
                }
                case "sudo" -> {
                    Player player = player(args[1]);
                    String line = String.join(" ", List.of(args).subList(2, args.length));
                    player.getScheduler().run(this, task -> {
                        player.performCommand(line.startsWith("/") ? line.substring(1) : line);
                        report("sudo " + player.getName() + " " + line);
                    }, () -> report("error sudo retired"));
                }
                case "hit" -> {
                    // The attacker hurts the victim (EntityDamageByEntityEvent), on the victim's thread.
                    Player attacker = player(args[1]);
                    Player victim = player(args[2]);
                    double amount = Double.parseDouble(args[3]);
                    victim.getScheduler().run(this, task -> {
                        victim.setNoDamageTicks(0);
                        double before = victim.getHealth();
                        victim.damage(amount, attacker);
                        report(String.format(Locale.ROOT, "hit %s %s before=%.1f after=%.1f", attacker.getName(), victim.getName(),
                                before, victim.getHealth()));
                        victim.setHealth(Math.min(20.0, victim.getHealth() + 20.0));
                    }, () -> report("error hit retired"));
                }
                case "kill" -> {
                    Player killer = player(args[1]);
                    Player victim = player(args[2]);
                    victim.getScheduler().run(this, task -> {
                        victim.setNoDamageTicks(0);
                        victim.damage(10_000, killer);
                        report("kill " + killer.getName() + " " + victim.getName() + " dead=" + victim.isDead());
                    }, () -> report("error kill retired"));
                }
                case "damage" -> {
                    Player player = player(args[1]);
                    double amount = Double.parseDouble(args[2]);
                    player.getScheduler().run(this, task -> {
                        player.setNoDamageTicks(0);
                        player.damage(amount);
                        report("damage " + player.getName() + " health=" + player.getHealth());
                    }, () -> report("error damage retired"));
                }
                case "give" -> {
                    Player player = player(args[1]);
                    Material material = Material.matchMaterial(args[2]);
                    int amount = Integer.parseInt(args[3]);
                    player.getScheduler().run(this, task -> {
                        player.getInventory().addItem(new ItemStack(material, amount));
                        report("give " + player.getName() + " " + material + " " + amount);
                    }, () -> report("error give retired"));
                }
                case "hold" -> {
                    // Puts an item in the main hand (for /team icon).
                    Player player = player(args[1]);
                    Material material = Material.matchMaterial(args[2]);
                    player.getScheduler().run(this, task -> {
                        player.getInventory().setItemInMainHand(new ItemStack(material));
                        report("hold " + player.getName() + " " + material);
                    }, () -> report("error hold retired"));
                }
                case "count" -> {
                    Player player = player(args[1]);
                    Material material = Material.matchMaterial(args[2]);
                    player.getScheduler().run(this, task -> {
                        int total = 0;
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType() == material) {
                                total += item.getAmount();
                            }
                        }
                        report("count " + player.getName() + " " + material + " " + total);
                    }, () -> report("error count retired"));
                }
                case "papi" -> {
                    Player player = player(args[1]);
                    String text = String.join(" ", List.of(args).subList(2, args.length));
                    report("papi " + player.getName() + " " + me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text));
                }
                case "rel" -> {
                    Player one = player(args[1]);
                    Player two = player(args[2]);
                    String text = String.join(" ", List.of(args).subList(3, args.length));
                    report("rel " + one.getName() + " " + two.getName() + " "
                            + me.clip.placeholderapi.PlaceholderAPI.setRelationalPlaceholders(one, two, text).replaceAll("§.", ""));
                }
                case "team" -> report(team(args[1]));
                case "teams" -> report("teams " + DonutTeams.getApi().teams().size());
                case "displays" -> {
                    World world = world(args[1]);
                    Location at = new Location(world, Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
                    Bukkit.getRegionScheduler().run(this, at, task -> {
                        List<String> texts = new ArrayList<>();
                        for (TextDisplay display : world.getNearbyEntitiesByType(TextDisplay.class, at, 4.0)) {
                            texts.add(PlainTextComponentSerializer.plainText().serialize(display.text()).replace('\n', '|'));
                        }
                        report("displays " + texts.size() + " " + String.join(" // ", texts));
                    });
                }
                case "set" -> {
                    File file = new File(getDataFolder().getParentFile(), "DonutTeams/" + args[1]);
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    String value = String.join(" ", List.of(args).subList(3, args.length));
                    yaml.set(args[2], parse(value));
                    yaml.save(file);
                    report("set " + args[1] + " " + args[2] + "=" + yaml.get(args[2]));
                }
                case "yaml" -> {
                    File file = new File(getDataFolder().getParentFile(), "DonutTeams/" + args[1]);
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    report("yaml " + args[1] + " " + args[2] + "=" + yaml.get(args[2]));
                }
                case "file" -> {
                    File file = new File(getDataFolder().getParentFile(), "DonutTeams/" + args[1]);
                    report("file " + args[1] + " exists=" + file.exists());
                }
                default -> report("error unknown " + sub);
            }
        } catch (Exception | LinkageError e) {
            report("error " + sub + " " + e);
        }
        return true;
    }

    /** One line about a team, from the public API. */
    private static String team(String name) {
        DonutTeamsApi api = DonutTeams.getApi();
        TeamSnapshot team = api.teamByName(name).orElse(null);
        if (team == null) {
            return "team " + name + " none";
        }
        String allies = team.allies().stream().map(id -> api.teamById(id).map(TeamSnapshot::name).orElse("?")).sorted()
                .collect(Collectors.joining(","));
        String enemies = team.enemies().stream().map(id -> api.teamById(id).map(TeamSnapshot::name).orElse("?")).sorted()
                .collect(Collectors.joining(","));
        String members = team.members().stream().map(TeamLabPlugin::name).sorted().collect(Collectors.joining(","));
        return String.format(Locale.ROOT, "team %s tag=%s owner=%s members=%s level=%d balance=%.2f kills=%d deaths=%d open=%s ff=%s "
                        + "color=%s allies=%s enemies=%s home=%s",
                team.name(), team.tag(), name(team.ownerId()), members, team.level(), team.balance(), team.kills(), team.deaths(),
                team.open(), team.friendlyFire(), team.color(), allies, enemies, team.homeWorld());
    }

    private static String name(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name == null ? id.toString() : name;
    }

    private static Player player(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            throw new IllegalArgumentException("no player " + name);
        }
        return player;
    }

    private static World world(String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            throw new IllegalArgumentException("no world " + name);
        }
        return world;
    }

    private static Object parse(String value) {
        if (value.equals("true") || value.equals("false")) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // not an int
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            // not a number
        }
        return value.replaceAll("^\"|\"$", "");
    }
}
