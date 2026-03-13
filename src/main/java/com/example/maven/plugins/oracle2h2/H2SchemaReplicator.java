package com.example.maven.plugins.oracle2h2;

import com.example.maven.plugins.oracle2h2.model.ColumnMetadata;
import com.example.maven.plugins.oracle2h2.model.SchemaMetadata;
import com.example.maven.plugins.oracle2h2.model.TableMetadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Replicates schema metadata (CREATE SCHEMA, CREATE TABLE) into an H2 database in Oracle mode.
 */
public class H2SchemaReplicator {

    private static final Pattern IDENT = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    public void replicate(Connection h2Conn, List<SchemaMetadata> schemas) throws SQLException {
        for (SchemaMetadata schema : schemas) {
            createSchema(h2Conn, schema.getSchemaName());
            for (TableMetadata table : schema.getTables()) {
                createTable(h2Conn, table);
            }
        }
    }

    private void createSchema(Connection conn, String schemaName) throws SQLException {
        if (schemaName == null || schemaName.isEmpty()) return;
        String safe = quoteIdentifier(schemaName);
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS " + safe);
        }
    }

    private void createTable(Connection conn, TableMetadata table) throws SQLException {
        String schema = table.getSchemaName();
        String tableName = table.getTableName();
        String fullName = qualifiedName(schema, tableName);

        StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(fullName).append(" (\n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            ColumnMetadata col = table.getColumns().get(i);
            if (i > 0) ddl.append(",\n");
            ddl.append("  ").append(quoteIdentifier(col.getName())).append(" ");
            ddl.append(OracleTypeMapper.toH2Type(col));
            if (!col.isNullable()) ddl.append(" NOT NULL");
            if (col.getDefaultValue() != null && !col.getDefaultValue().isEmpty()) {
                ddl.append(" DEFAULT ").append(sanitizeDefault(col.getDefaultValue()));
            }
        }
        if (!table.getPrimaryKeyColumns().isEmpty()) {
            ddl.append(",\n  CONSTRAINT ").append(quoteIdentifier("PK_" + tableName)).append(" PRIMARY KEY (");
            for (int i = 0; i < table.getPrimaryKeyColumns().size(); i++) {
                if (i > 0) ddl.append(", ");
                ddl.append(quoteIdentifier(table.getPrimaryKeyColumns().get(i)));
            }
            ddl.append(")");
        }
        ddl.append("\n)");

        try (Statement st = conn.createStatement()) {
            st.execute(ddl.toString());
        }
    }

    private String qualifiedName(String schema, String tableName) {
        return quoteIdentifier(schema) + "." + quoteIdentifier(tableName);
    }

    private String quoteIdentifier(String name) {
        if (name == null) return "\"\"";
        if (IDENT.matcher(name).matches()) return name;
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private String sanitizeDefault(String defaultValue) {
        if (defaultValue == null) return "NULL";
        String s = defaultValue.trim();
        if (s.equalsIgnoreCase("NULL")) return "NULL";
        if (s.toUpperCase().startsWith("SYSDATE") || s.toUpperCase().contains("SYSTIMESTAMP")) return "CURRENT_TIMESTAMP";
        if (s.toUpperCase().startsWith("SYS_GUID()")) return "RANDOM_UUID()";
        return s;
    }
}
