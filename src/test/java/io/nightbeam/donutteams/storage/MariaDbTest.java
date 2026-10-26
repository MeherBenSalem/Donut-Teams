package io.nightbeam.donutteams.storage;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import org.junit.jupiter.api.Test;

/** Runs the repository contract on a real MariaDB/MySQL when DONUTTEAMS_MARIADB_URL is set (user and password too). */
class MariaDbTest {

    @Test
    void repositoryRoundTripsOnMariaDb() throws Exception {
        String url = System.getenv("DONUTTEAMS_MARIADB_URL");
        assumeTrue(url != null && !url.isBlank(), "DONUTTEAMS_MARIADB_URL not set");
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(org.mariadb.jdbc.Driver.class.getName());
        config.setJdbcUrl(url);
        config.setUsername(System.getenv().getOrDefault("DONUTTEAMS_MARIADB_USER", "root"));
        config.setPassword(System.getenv().getOrDefault("DONUTTEAMS_MARIADB_PASSWORD", ""));
        config.setMaximumPoolSize(2);
        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            String prefix = "t" + Long.toString(System.nanoTime(), 36) + "_";
            try (Connection connection = dataSource.getConnection()) {
                Schema.apply(connection, DatabaseType.MARIADB, prefix, message -> { });
                Schema.apply(connection, DatabaseType.MARIADB, prefix, message -> { });
            }
            RepositoryContract.run(new TeamRepository(dataSource, DatabaseType.MARIADB, prefix));
        }
    }
}
