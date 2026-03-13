package com.example.maven.plugins.oracle2h2;

import com.example.maven.plugins.oracle2h2.model.ColumnMetadata;
import com.example.maven.plugins.oracle2h2.model.SchemaMetadata;
import com.example.maven.plugins.oracle2h2.model.TableMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit/integration tests for {@link H2SchemaReplicator} using an in-memory H2 database.
 */
public class H2SchemaReplicatorTest {

    private static final String H2_URL = "jdbc:h2:mem:test_replicator;MODE=Oracle;DB_CLOSE_DELAY=-1";

    private Connection conn;
    private H2SchemaReplicator replicator;

    @Before
    public void setUp() throws SQLException {
        conn = DriverManager.getConnection(H2_URL, "SA", "");
        replicator = new H2SchemaReplicator();
    }

    @After
    public void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    public void replicateCreatesSchemaAndTable() throws SQLException {
        SchemaMetadata schema = new SchemaMetadata("MYSCHEMA");
        TableMetadata table = new TableMetadata("MYSCHEMA", "MYTABLE", "TABLE");
        table.addColumn(new ColumnMetadata("ID", "NUMBER", java.sql.Types.NUMERIC, 10, 0, false, null, false));
        table.addColumn(new ColumnMetadata("NAME", "VARCHAR2", java.sql.Types.VARCHAR, 100, 0, true, null, false));
        table.addPrimaryKeyColumn("ID");
        schema.addTable(table);

        replicator.replicate(conn, Collections.singletonList(schema));

        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'MYSCHEMA' AND TABLE_NAME = 'MYTABLE'");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT ID, NAME FROM MYSCHEMA.MYTABLE");
            assertEquals(2, rs.getMetaData().getColumnCount());
        }
    }

    @Test
    public void replicateMultipleSchemas() throws SQLException {
        SchemaMetadata s1 = new SchemaMetadata("SCHEMA_A");
        TableMetadata t1 = new TableMetadata("SCHEMA_A", "T1", "TABLE");
        t1.addColumn(new ColumnMetadata("ID", "NUMBER", java.sql.Types.INTEGER, 10, 0, false, null, false));
        s1.addTable(t1);

        SchemaMetadata s2 = new SchemaMetadata("SCHEMA_B");
        TableMetadata t2 = new TableMetadata("SCHEMA_B", "T2", "TABLE");
        t2.addColumn(new ColumnMetadata("ID", "NUMBER", java.sql.Types.INTEGER, 10, 0, false, null, false));
        s2.addTable(t2);

        replicator.replicate(conn, java.util.Arrays.asList(s1, s2));

        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA IN ('SCHEMA_A','SCHEMA_B') ORDER BY TABLE_SCHEMA");
            assertTrue(rs.next());
            assertEquals("SCHEMA_A", rs.getString(1));
            assertEquals("T1", rs.getString(2));
            assertTrue(rs.next());
            assertEquals("SCHEMA_B", rs.getString(1));
            assertEquals("T2", rs.getString(2));
        }
    }

    @Test
    public void replicateTableWithDefaultValue() throws SQLException {
        SchemaMetadata schema = new SchemaMetadata("APP");
        TableMetadata table = new TableMetadata("APP", "CONFIG", "TABLE");
        table.addColumn(new ColumnMetadata("K", "VARCHAR2", java.sql.Types.VARCHAR, 50, 0, false, null, false));
        table.addColumn(new ColumnMetadata("V", "VARCHAR2", java.sql.Types.VARCHAR, 200, 0, true, "'default'", false));
        table.addPrimaryKeyColumn("K");
        schema.addTable(table);

        replicator.replicate(conn, Collections.singletonList(schema));

        try (Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO APP.CONFIG (K, V) VALUES ('x', 'y')");
            ResultSet rs = st.executeQuery("SELECT K, V FROM APP.CONFIG WHERE K = 'x'");
            assertTrue(rs.next());
            assertEquals("x", rs.getString(1));
            assertEquals("y", rs.getString(2));
        }
    }
}
