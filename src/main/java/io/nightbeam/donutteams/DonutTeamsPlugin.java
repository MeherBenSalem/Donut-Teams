package io.nightbeam.donutteams;

import io.nightbeam.donutteams.api.DonutTeamsApi;
import io.nightbeam.donutteams.api.DonutTeamsApiImpl;
import io.nightbeam.donutteams.command.DonutTeamsCommand;
import io.nightbeam.donutteams.command.TeamCommand;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.PluginSettings;
import io.nightbeam.donutteams.gui.GuiManager;
import io.nightbeam.donutteams.hook.DonutCoreHook;
import io.nightbeam.donutteams.hook.HookManager;
import io.nightbeam.donutteams.hook.LuckPermsHook;
import io.nightbeam.donutteams.hook.TeamsPlaceholderExpansion;
import io.nightbeam.donutteams.listener.ChatListener;
import io.nightbeam.donutteams.listener.FriendlyFireListener;
import io.nightbeam.donutteams.listener.PlayerJoinListener;
import io.nightbeam.donutteams.listener.PlayerMoveListener;
import io.nightbeam.donutteams.listener.PlayerQuitListener;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.service.ChatService;
import io.nightbeam.donutteams.service.HomeWarmupService;
import io.nightbeam.donutteams.service.SlotService;
import io.nightbeam.donutteams.service.TeamCache;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.storage.DatabaseManager;
import io.nightbeam.donutteams.storage.SqlTeamRepository;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutTeamsPlugin extends JavaPlugin {

    private FoliaScheduler scheduler;
    private PluginSettings settings;
    private Messages messages;
    private DatabaseManager database;
    private TeamService teams;
    private ChatService chat;
    private HomeWarmupService homes;
    private GuiManager guis;
    private DonutCoreHook donutCore;
    private FileConfiguration messagesYaml;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfAbsent("messages.yml");
        this.settings = new PluginSettings(this);
        this.messages = new Messages(this);
        reloadMessagesYaml();

        this.scheduler = new FoliaScheduler(this);
        this.donutCore = HookManager.donutCore(this);
        LuckPermsHook luckPerms = new LuckPermsHook();

        this.database = DatabaseManager.fromSettings(this, settings);
        try {
            this.database.initialize();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to initialize Donut Teams storage", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SqlTeamRepository repository = new SqlTeamRepository(database.dataSource(), database.type());
        TeamCache cache = new TeamCache();
        SlotService slots = new SlotService(settings, luckPerms);
        this.teams = new TeamService(this, settings, messages, scheduler, repository, cache, slots, donutCore);
        try {
            this.teams.load();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to load teams", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.chat = new ChatService(settings, messages, scheduler, teams);
        this.homes = new HomeWarmupService(settings, messages, scheduler, teams);
        this.guis = new GuiManager(this, messages, scheduler, teams, chat, homes);

        DonutTeamsApi api = new DonutTeamsApiImpl(teams);
        getServer().getServicesManager().register(DonutTeamsApi.class, api, this, ServicePriority.Normal);

        registerCommands();
        registerListeners();
        registerPlaceholders();
        startMetrics();

        getLogger().info("Donut Teams lite 1.0.0 enabled. Folia=" + scheduler.isFolia()
                + " storage=" + database.type()
                + " DonutCore=" + donutCore.isAvailable()
                + " LuckPerms=" + luckPerms.isAvailable()
                + " Vault=" + HookManager.present("Vault")
                + " PlaceholderAPI=" + HookManager.present("PlaceholderAPI"));
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (database != null) {
            database.shutdown();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadMessagesYaml();
    }

    public Messages messages() {
        return messages;
    }

    public PluginSettings settings() {
        return settings;
    }

    public FoliaScheduler scheduler() {
        return scheduler;
    }

    public TeamService teams() {
        return teams;
    }

    public GuiManager guis() {
        return guis;
    }

    public FileConfiguration messagesYaml() {
        return messagesYaml;
    }

    private void registerCommands() {
        TeamCommand teamCommand = new TeamCommand(this, teams, chat, homes, guis);
        PluginCommand team = getCommand("team");
        if (team != null) {
            team.setExecutor(teamCommand);
            team.setTabCompleter(teamCommand);
        } else {
            getLogger().severe("Command 'team' is missing from plugin.yml");
        }
        DonutTeamsCommand admin = new DonutTeamsCommand(this);
        PluginCommand donutTeams = getCommand("donutteams");
        if (donutTeams != null) {
            donutTeams.setExecutor(admin);
            donutTeams.setTabCompleter(admin);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(guis, this);
        getServer().getPluginManager().registerEvents(new ChatListener(chat, scheduler), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(teams), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(homes), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(chat, homes), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(scheduler, teams), this);
    }

    private void registerPlaceholders() {
        if (!HookManager.present("PlaceholderAPI")) {
            return;
        }
        try {
            new TeamsPlaceholderExpansion(this, teams).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        } catch (Throwable throwable) {
            getLogger().warning("Could not register PlaceholderAPI expansion: " + throwable.getMessage());
        }
    }

    private void startMetrics() {
        if (!settings.metricsEnabled() || settings.bstatsId() <= 0) {
            getLogger().info("bStats stub skipped (metrics disabled or id unset).");
            return;
        }
        try {
            new Metrics(this, settings.bstatsId());
        } catch (Throwable throwable) {
            getLogger().warning("Failed to start bStats: " + throwable.getMessage());
        }
    }

    private void reloadMessagesYaml() {
        File file = new File(getDataFolder(), "messages.yml");
        this.messagesYaml = YamlConfiguration.loadConfiguration(file);
        try (var reader = new InputStreamReader(getResource("messages.yml"), StandardCharsets.UTF_8)) {
            this.messagesYaml.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (Exception ignored) {
            // Jar defaults are optional if the resource is missing.
        }
        messages.load(this.messagesYaml);
    }

    private void saveResourceIfAbsent(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }
}
