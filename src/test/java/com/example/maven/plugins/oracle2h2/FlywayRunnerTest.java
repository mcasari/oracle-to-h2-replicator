package com.example.maven.plugins.oracle2h2;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration-style unit test for {@link FlywayRunner}: runs Flyway against in-memory H2
 * with a test migration from classpath.
 */
public class FlywayRunnerTest {

    private static final String H2_URL = "jdbc:h2:mem:flyway_test;MODE=Oracle;DB_CLOSE_DELAY=-1";

    @Test
    public void runAppliesMigrationsFromClasspath() throws Exception {
        SystemStreamLog log = new SystemStreamLog();
        FlywayRunner.run(H2_URL, "SA", "", Collections.singletonList("classpath:db/migration"), true, log);

        try (Connection conn = DriverManager.getConnection(H2_URL, "SA", "");
             ResultSet rs = conn.createStatement().executeQuery("SELECT ID, LABEL FROM TEST_BASE WHERE ID = 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("ID"));
            assertEquals("base", rs.getString("LABEL"));
        }
    }
}
