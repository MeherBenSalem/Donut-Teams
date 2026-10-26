package io.nightbeam.donutteams;

import io.nightbeam.donutteams.api.DonutTeamsApi;
import io.nightbeam.donutteams.api.DonutTeamsApiImpl;
import io.nightbeam.donutteams.board.BoardService;
import io.nightbeam.donutteams.command.AdminCommand;
import io.nightbeam.donutteams.command.ChatCommand;
import io.nightbeam.donutteams.command.TeamCommand;
import io.nightbeam.donutteams.config.ConfigFiles;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.gui.GuiManager;
import io.nightbeam.donutteams.hook.DonutCoreHook;
import io.nightbeam.donutteams.hook.Economy;
import io.nightbeam.donutteams.hook.EconomyHook;
import io.nightbeam.donutteams.hook.HookManager;
import io.nightbeam.donutteams.hook.TeamsPlaceholderExpansion;
import io.nightbeam.donutteams.hook.UpdateChecker;
import io.nightbeam.donutteams.listener.ChatListener;
import io.nightbeam.donutteams.listener.CombatListener;
import io.nightbeam.donutteams.listener.PlayerListener;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.service.BankService;
import io.nightbeam.donutteams.service.ChatService;
import io.nightbeam.donutteams.service.ChestService;
import io.nightbeam.donutteams.service.Format;
import io.nightbeam.donutteams.service.HomeService;
import io.nightbeam.donutteams.service.InputService;
import io.nightbeam.donutteams.service.RelationService;
import io.nightbeam.donutteams.service.SoundService;
import io.nightbeam.donutteams.service.TeamCache;
import io.nightbeam.donutteams.service.TeamService;
import io.nightbeam.donutteams.service.TopService;
import io.nightbeam.donutteams.storage.DataWriter;
import io.nightbeam.donutteams.storage.DatabaseManager;
import io.nightbeam.donutteams.storage.TeamRepository;
import java.util.List;
import java.util.logging.Level;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutTeamsPlugin extends JavaPlugin {

    /** bStats plugin id (0 = metrics off until the project has one). */
    private static final int BSTATS_ID = 0;

    private volatile Settings settings = Settings.defaults();
    private final Messages messages = new Messages();
    private FoliaScheduler scheduler;
    private SoundService sounds;
    private Economy economy;
    private DatabaseManager database;
    private DataWriter writer;
    private TeamService teams;
    private RelationService relations;
    private BankService bank;
    private ChestService chests;
    private HomeService homes;
    private ChatService chat;
    private TopService top;
    private InputService input;
    private GuiManager guis;
    private BoardService boards;
    private UpdateChecker updates;
    private TeamCommand teamCommand;
    private FoliaScheduler.Task topTask = FoliaScheduler.Task.NONE;
    private FoliaScheduler.Task purgeTask = FoliaScheduler.Task.NONE;
    private FoliaScheduler.Task chestTask = FoliaScheduler.Task.NONE;
    private boolean enabled;
    private Runnable luckPermsContexts;

    @Override
    public void onEnable() {
        if (!paperApiPresent()) {
            getLogger().severe("Donut Teams needs Paper or a Paper fork (Purpur, Folia, Pufferfish...) 1.20.1 or newer. "
                    + "Spigot and CraftBukkit are not supported.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        List<String> notes = ConfigFiles.prepare(this);
        reloadConfig();
        this.settings = Settings.from(getConfig());
        this.scheduler = new FoliaScheduler(this);
        this.sounds = new SoundService(scheduler);
        this.guis = new GuiManager(this);
        loadTexts();

        this.economy = new EconomyHook();
        DonutCoreHook donutCore = HookManager.donutCore(this);

        this.database = DatabaseManager.fromSettings(this, settings);
        try {
            database.initialize();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Could not open the Donut Teams database (" + settings.storageType + "). "
                    + "Check storage in config.yml.", exception);
            database.shutdown();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        TeamRepository repository = new TeamRepository(database.dataSource(), database.type(), database.prefix());
        this.writer = new DataWriter(repository, getLogger());
        TeamCache cache = new TeamCache();
        Format format = new Format(this::settings, messages, economy);
        this.teams = new TeamService(this, this::settings, messages, format, scheduler, writer, cache, donutCore, economy);
        try {
            teams.load();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Could not load teams", exception);
            writer.shutdown();
            database.shutdown();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.relations = new RelationService(this::settings, messages, teams, writer);
        this.bank = new BankService(this, this::settings, messages, teams, economy, writer);
        this.chests = new ChestService(this, this::settings, messages, scheduler, teams, writer);
        teams.chests(chests);
        this.homes = new HomeService(this, this::settings, messages, scheduler, teams, writer);
        this.chat = new ChatService(this, this::settings, messages, scheduler, teams);
        this.top = new TopService(cache);
        top.refresh();
        this.input = new InputService(this, DialogSupport.create(this));
        this.boards = new BoardService(this, scheduler);
        this.updates = new UpdateChecker(this);

        getServer().getServicesManager().register(DonutTeamsApi.class, new DonutTeamsApiImpl(teams), this, ServicePriority.Normal);
        registerCommands();
        getServer().getPluginManager().registerEvents(guis, this);
        getServer().getPluginManager().registerEvents(new ChatListener(chat, scheduler), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        registerPlaceholders();
        registerLuckPerms();
        startTimers();
        startMetrics();
        updates.check();
        enabled = true;

        for (String note : notes) {
            getLogger().info(note);
        }
        if (database.foundVersion() == 1) {
            getLogger().info("Your 1.x teams, members, invites and homes were kept. The old home is now called \"home\".");
        }
        getLogger().info("Donut Teams " + getPluginMeta().getVersion() + " enabled on " + platform() + ": " + cache.size()
                + (cache.size() == 1 ? " team" : " teams") + ", storage " + storageName()
                + ", economy " + (economy.available() ? economy.name() : "none")
                + ", dialogs " + input.dialogsAvailable() + ", PlaceholderAPI " + HookManager.present("PlaceholderAPI")
                + ", LuckPerms contexts " + (luckPermsContexts != null)
                + ", DonutCore " + donutCore.isAvailable() + ".");
    }

    @Override
    public void onDisable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        topTask.cancel();
        purgeTask.cancel();
        chestTask.cancel();
        if (boards != null) {
            boards.shutdown();
        }
        if (chests != null) {
            chests.saveAll();
        }
        getServer().getServicesManager().unregisterAll(this);
        if (luckPermsContexts != null) {
            try {
                luckPermsContexts.run();
            } catch (Throwable ignored) {
                // LuckPerms may already be gone.
            }
        }
        if (writer != null) {
            writer.shutdown();
        }
        if (database != null) {
            database.shutdown();
        }
    }

    /** /donutteams reload: config, messages, menus and sounds (storage settings need a restart). */
    public void reloadPlugin() {
        ConfigFiles.prepare(this);
        reloadConfig();
        this.settings = Settings.from(getConfig());
        loadTexts();
        startTimers();
        top.refresh();
    }

    private void loadTexts() {
        messages.load(ConfigFiles.load(this, "messages.yml"));
        guis.config().load(ConfigFiles.load(this, "menus.yml"));
        sounds.load(getConfig().getConfigurationSection("sounds"));
    }

    private void startTimers() {
        topTask.cancel();
        purgeTask.cancel();
        chestTask.cancel();
        topTask = scheduler.runAsyncTimer(top::refresh, settings.leaderboardRefreshSeconds, settings.leaderboardRefreshSeconds);
        purgeTask = scheduler.runAsyncTimer(() -> {
            int removed = teams.cache().purgeExpiredInvites();
            if (removed > 0) {
                long now = System.currentTimeMillis();
                writer.write("expired invites", repository -> repository.deleteExpiredInvites(now));
            }
        }, 60, 60);
        chestTask = scheduler.runAsyncTimer(chests::autosave, settings.chestAutosaveSeconds, settings.chestAutosaveSeconds);
        boards.start(settings.boardRefreshSeconds);
    }

    private void registerCommands() {
        this.teamCommand = new TeamCommand(this);
        bind("team", teamCommand, teamCommand);
        AdminCommand admin = new AdminCommand(this);
        bind("donutteams", admin, admin);
        ChatCommand teamChat = new ChatCommand(this, false);
        bind("teamchat", teamChat, teamChat);
        ChatCommand allyChat = new ChatCommand(this, true);
        bind("allychat", allyChat, allyChat);
    }

    private void bind(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(completer);
    }

    private void registerPlaceholders() {
        if (!HookManager.present("PlaceholderAPI")) {
            return;
        }
        try {
            new TeamsPlaceholderExpansion(this).register();
        } catch (Throwable throwable) {
            getLogger().warning("Could not register the PlaceholderAPI expansion: " + throwable.getMessage());
        }
    }

    private void registerLuckPerms() {
        if (!HookManager.present("LuckPerms")) {
            return;
        }
        try {
            luckPermsContexts = io.nightbeam.donutteams.hook.LuckPermsContexts.register(teams);
        } catch (Throwable throwable) {
            getLogger().warning("Could not register LuckPerms contexts: " + throwable.getMessage());
        }
    }

    private void startMetrics() {
        if (!settings.metrics || BSTATS_ID <= 0) {
            return;
        }
        try {
            new Metrics(this, BSTATS_ID);
        } catch (Throwable throwable) {
            getLogger().fine("bStats did not start: " + throwable.getMessage());
        }
    }

    /** A few lines about the running install, for /donutteams info. */
    public void report(CommandSender sender) {
        int members = 0;
        int homesCount = 0;
        int alliances = 0;
        for (Team team : teams.cache().teams()) {
            members += team.size();
            homesCount += team.homes().size();
            alliances += team.allies().size();
        }
        sender.sendMessage(messages.render("<gray>Teams <white>" + teams.cache().size() + "</white> · members <white>" + members
                + "</white> · homes <white>" + homesCount + "</white> · alliances <white>" + alliances / 2
                + "</white> · boards <white>" + boards.names().size() + "</white> · dialogs <white>" + input.dialogsAvailable()
                + "</white> · PlaceholderAPI <white>" + HookManager.present("PlaceholderAPI") + "</white>"));
    }

    public String platform() {
        String version = Bukkit.getMinecraftVersion();
        return (scheduler != null && scheduler.isFolia() ? "Folia " : Bukkit.getName() + " ") + version;
    }

    public String storageName() {
        return database == null ? "?" : database.type() == io.nightbeam.donutteams.storage.DatabaseType.MARIADB ? "MariaDB/MySQL" : "SQLite";
    }

    private static boolean paperApiPresent() {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    // ── Accessors ───────────────────────────────────────────────────────────────────────────────────────────

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public FoliaScheduler scheduler() {
        return scheduler;
    }

    public SoundService sounds() {
        return sounds;
    }

    public Economy economy() {
        return economy;
    }

    public TeamService teams() {
        return teams;
    }

    public RelationService relations() {
        return relations;
    }

    public BankService bank() {
        return bank;
    }

    public ChestService chests() {
        return chests;
    }

    public HomeService homes() {
        return homes;
    }

    public ChatService chat() {
        return chat;
    }

    public TopService top() {
        return top;
    }

    public InputService input() {
        return input;
    }

    public GuiManager guis() {
        return guis;
    }

    public BoardService boards() {
        return boards;
    }

    public UpdateChecker updates() {
        return updates;
    }

    public TeamCommand teamCommand() {
        return teamCommand;
    }
}
