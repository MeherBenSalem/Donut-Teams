package io.nightbeam.donutteams.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.nightbeam.donutteams.config.PluginSettings;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.bukkit.plugin.java.JavaPlugin;

public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final DatabaseType type;
    private final HikariDataSource dataSource;

    private DatabaseManager(JavaPlugin plugin, DatabaseType type, HikariDataSource dataSource) {
        this.plugin = plugin;
        this.type = type;
        this.dataSource = dataSource;
    }

    public static DatabaseManager fromSettings(JavaPlugin plugin, PluginSettings settings) {
        plugin.getDataFolder().mkdirs();
        DatabaseType type = Schema.fromSettings(settings);
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("DonutTeamsPool");
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(10_000L);
        hikari.setValidationTimeout(5_000L);
        hikari.setLeakDetectionThreshold(30_000L);

        if (type == DatabaseType.SQLITE) {
            File dataFolder = plugin.getDataFolder();
            System.setProperty("org.sqlite.tmpdir", dataFolder.getAbsolutePath());
            File file = new File(dataFolder, settings.sqliteFile());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikari.setMaximumPoolSize(1);
            hikari.setConnectionInitSql("PRAGMA foreign_keys = ON");
            hikari.addDataSourceProperty("journal_mode", "WAL");
            hikari.addDataSourceProperty("synchronous", "NORMAL");
            hikari.addDataSourceProperty("busy_timeout", "5000");
        } else {
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setJdbcUrl("jdbc:mysql://" + settings.mysqlHost() + ":" + settings.mysqlPort()
                    + "/" + settings.mysqlDatabase() + "?" + settings.mysqlParameters());
            hikari.setUsername(settings.mysqlUsername());
            hikari.setPassword(settings.mysqlPassword());
            hikari.setMaximumPoolSize(10);
        }
        return new DatabaseManager(plugin, type, new HikariDataSource(hikari));
    }

    public void initialize() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Schema.apply(connection, type);
        }
        plugin.getLogger().info("Donut Teams storage ready (" + type.name() + ").");
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public DatabaseType type() {
        return type;
    }

    public void shutdown() {
        dataSource.close();
    }
}
