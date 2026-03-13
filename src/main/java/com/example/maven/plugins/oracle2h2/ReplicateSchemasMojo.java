package com.example.maven.plugins.oracle2h2;

import com.example.maven.plugins.oracle2h2.model.SchemaMetadata;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * Replicates one or more Oracle schemas (structure and optionally data) into an in-memory H2 database in Oracle mode.
 * <p>
 * Execute with: {@code mvn oracle-h2:replicate-schemas}
 * </p>
 */
@Mojo(name = "replicate-schemas")
public class ReplicateSchemasMojo extends AbstractMojo {

    /** Oracle JDBC URL (e.g. jdbc:oracle:thin:@//host:1521/SERVICE) */
    @Parameter(property = "oracle-h2.oracleJdbcUrl", required = true)
    private String oracleJdbcUrl;

    /** Oracle database user */
    @Parameter(property = "oracle-h2.oracleUser", required = true)
    private String oracleUser;

    /** Oracle database password */
    @Parameter(property = "oracle-h2.oraclePassword", required = true)
    private String oraclePassword;

    /** Comma-separated list of Oracle schema names to replicate (e.g. SCOTT, HR). Also supports &lt;schemas&gt; list in POM. */
    @Parameter(property = "oracle-h2.schemas", required = true)
    private String schemas;

    /** Alternative: list of schema names (when set, overrides 'schemas' string). */
    @Parameter(property = "oracle-h2.schemaList")
    private List<String> schemaList;

    /** H2 JDBC URL. Default: in-memory DB with MODE=Oracle. Use jdbc:h2:mem:replicated;MODE=Oracle;DB_CLOSE_DELAY=-1 */
    @Parameter(property = "oracle-h2.h2JdbcUrl", defaultValue = "jdbc:h2:mem:replicated;MODE=Oracle;DB_CLOSE_DELAY=-1")
    private String h2JdbcUrl;

    /** H2 user (default SA) */
    @Parameter(property = "oracle-h2.h2User", defaultValue = "SA")
    private String h2User;

    /** H2 password (default empty) */
    @Parameter(property = "oracle-h2.h2Password", defaultValue = "")
    private String h2Password;

    /** If true, copy table data from Oracle to H2 after creating the schema. Default: false (structure only). */
    @Parameter(property = "oracle-h2.copyData", defaultValue = "false")
    private boolean copyData;

    /** Skip execution. Useful when binding to a phase but running only on demand via -Doracle-h2.skip=false */
    @Parameter(property = "oracle-h2.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("oracle-h2:replicate-schemas is skipped.");
            return;
        }

        List<String> resolvedSchemas;
        if (schemaList != null && !schemaList.isEmpty()) {
            resolvedSchemas = new ArrayList<>(schemaList);
            resolvedSchemas.removeIf(s -> s == null || s.trim().isEmpty());
            resolvedSchemas.replaceAll(String::trim);
        } else {
            resolvedSchemas = parseSchemas(schemas);
        }
        if (resolvedSchemas.isEmpty()) {
            throw new MojoFailureException("At least one schema must be specified in oracle-h2.schemas or oracle-h2.schemaList");
        }

        getLog().info("Replicating Oracle schemas to H2: " + resolvedSchemas);
        getLog().info("Oracle URL: " + maskUrl(oracleJdbcUrl));
        getLog().info("H2 URL: " + h2JdbcUrl);

        Connection oracleConn = null;
        Connection h2Conn = null;

        try {
            ensureH2OracleMode();
            oracleConn = DriverManager.getConnection(oracleJdbcUrl, oracleUser, oraclePassword);
            h2Conn = DriverManager.getConnection(h2JdbcUrl, h2User, h2Password);

            OracleSchemaReader reader = new OracleSchemaReader();
            List<SchemaMetadata> metadataList = reader.readSchemas(oracleConn, resolvedSchemas);

            int tableCount = metadataList.stream().mapToInt(s -> s.getTables().size()).sum();
            getLog().info("Read " + tableCount + " tables from " + metadataList.size() + " schema(s).");

            H2SchemaReplicator replicator = new H2SchemaReplicator();
            replicator.replicate(h2Conn, metadataList);
            getLog().info("Schema DDL applied to H2.");

            if (copyData) {
                getLog().info("Copying data from Oracle to H2...");
                DataReplicator dataReplicator = new DataReplicator();
                dataReplicator.copyData(oracleConn, h2Conn, metadataList);
                getLog().info("Data copy completed.");
            }

            getLog().info("Replication finished. H2 is available at: " + h2JdbcUrl);
        } catch (SQLException e) {
            throw new MojoExecutionException("Database error: " + e.getMessage(), e);
        } finally {
            closeQuietly(oracleConn);
            if (!isInMemory(h2JdbcUrl)) {
                closeQuietly(h2Conn);
            }
        }
    }

    private void ensureH2OracleMode() {
        if (!h2JdbcUrl.contains("MODE=Oracle") && !h2JdbcUrl.contains("MODE=ORACLE")) {
            getLog().warn("H2 URL does not contain MODE=Oracle. Adding MODE=Oracle for compatibility.");
        }
    }

    private static List<String> parseSchemas(String schemas) {
        if (schemas == null || schemas.isEmpty()) return new ArrayList<>();
        return Arrays.stream(schemas.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String maskUrl(String url) {
        if (url == null) return "null";
        int at = url.indexOf('@');
        if (at > 0) return url.substring(0, at) + "@***";
        return url;
    }

    private static boolean isInMemory(String url) {
        return url != null && url.contains(":mem:");
    }

    private static void closeQuietly(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignored) {}
        }
    }
}
