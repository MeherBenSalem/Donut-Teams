package io.nightbeam.donutteams.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.nightbeam.donutteams.config.Settings;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.bukkit.plugin.java.JavaPlugin;

/** The connection pool: SQLite in the plugin folder, or a MariaDB/MySQL server. */
public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final DatabaseType type;
    private final String prefix;
    private final HikariDataSource dataSource;
    private int foundVersion;

    private DatabaseManager(JavaPlugin plugin, DatabaseType type, String prefix, HikariDataSource dataSource) {
        this.plugin = plugin;
        this.type = type;
        this.prefix = prefix;
        this.dataSource = dataSource;
    }

    public static DatabaseManager fromSettings(JavaPlugin plugin, Settings settings) {
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("DonutTeamsPool");
        hikari.setConnectionTimeout(10_000L);
        hikari.setValidationTimeout(5_000L);
        hikari.setLeakDetectionThreshold(60_000L);
        if (settings.storageType == DatabaseType.MARIADB) {
            hikari.setDriverClassName(org.mariadb.jdbc.Driver.class.getName());
            String parameters = settings.dbParameters == null || settings.dbParameters.isBlank() ? "" : "?" + settings.dbParameters;
            hikari.setJdbcUrl("jdbc:mariadb://" + settings.dbHost + ":" + settings.dbPort + "/" + settings.dbName + parameters);
            hikari.setUsername(settings.dbUser);
            hikari.setPassword(settings.dbPassword);
            hikari.setMaximumPoolSize(settings.dbPoolSize);
            hikari.setMinimumIdle(1);
            hikari.setMaxLifetime(1_500_000L);
            hikari.setKeepaliveTime(300_000L);
        } else {
            System.setProperty("org.sqlite.tmpdir", dataFolder.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, settings.sqliteFile).getAbsolutePath());
            // One connection: SQLite has one writer, and every write comes from the plugin's database thread.
            hikari.setMaximumPoolSize(1);
            hikari.setMinimumIdle(1);
            hikari.addDataSourceProperty("journal_mode", "WAL");
            hikari.addDataSourceProperty("synchronous", "NORMAL");
            hikari.addDataSourceProperty("busy_timeout", "5000");
        }
        return new DatabaseManager(plugin, settings.storageType, settings.tablePrefix, new HikariDataSource(hikari));
    }

    public void initialize() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            foundVersion = Schema.apply(connection, type, prefix, message -> plugin.getLogger().info(message));
        }
    }

    /** The schema version found at start-up: 0 for a new database, 1 for a 1.x database (now upgraded). */
    public int foundVersion() {
        return foundVersion;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public DatabaseType type() {
        return type;
    }

    public String prefix() {
        return prefix;
    }

    public void shutdown() {
        dataSource.close();
    }
}
