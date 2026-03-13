package com.example.maven.plugins.oracle2h2;

import org.apache.maven.plugin.MojoExecutionException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import java.util.List;

/**
 * Runs Flyway migrations against the replicated H2 database to populate base data.
 */
public final class FlywayRunner {

    private FlywayRunner() {}

    /**
     * Runs Flyway migrate against the given H2 data source.
     *
     * @param jdbcUrl  H2 JDBC URL (must use same in-memory or file DB as replication)
     * @param user     H2 user
     * @param password H2 password
     * @param locations locations for migration scripts (e.g. "classpath:db/migration")
     * @param baselineOnMigrate if true, baseline existing schema instead of failing
     * @param log      mojo getLog() for output
     */
    public static void run(String jdbcUrl, String user, String password,
                           List<String> locations, boolean baselineOnMigrate,
                           org.apache.maven.plugin.logging.Log log) throws MojoExecutionException {
        if (locations == null || locations.isEmpty()) {
            log.warn("Flyway locations are empty; using default classpath:db/migration");
            locations = java.util.Collections.singletonList("classpath:db/migration");
        }
        try {
            FluentConfiguration config = Flyway.configure()
                    .dataSource(jdbcUrl, user, password != null ? password : "")
                    .locations(locations.toArray(new String[0]))
                    .baselineOnMigrate(baselineOnMigrate);
            Flyway flyway = config.load();
            flyway.migrate();
            log.info("Flyway migrations completed successfully.");
        } catch (Exception e) {
            throw new MojoExecutionException("Flyway migration failed: " + e.getMessage(), e);
        }
    }
}
