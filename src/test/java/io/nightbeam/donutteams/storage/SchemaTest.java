package io.nightbeam.donutteams.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

class SchemaTest {

    @Test
    void sqliteCreatesRequiredTables() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            Schema.apply(connection, DatabaseType.SQLITE);
            assertTrue(tableExists(connection, "teams"));
            assertTrue(tableExists(connection, "members"));
            assertTrue(tableExists(connection, "invites"));
            assertTrue(tableExists(connection, "homes"));
            assertTrue(tableExists(connection, "settings"));
        }
    }

    private static boolean tableExists(Connection connection, String name) throws Exception {
        try (ResultSet results = connection.getMetaData().getTables(null, null, name, null)) {
            return results.next();
        }
    }
}
