package com.example.maven.plugins.oracle2h2;

import com.example.maven.plugins.oracle2h2.model.ColumnMetadata;
import com.example.maven.plugins.oracle2h2.model.SchemaMetadata;
import com.example.maven.plugins.oracle2h2.model.TableMetadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads schema metadata (tables, columns, primary keys) from an Oracle database.
 */
public class OracleSchemaReader {

    public List<SchemaMetadata> readSchemas(Connection oracleConn, List<String> schemaNames) throws SQLException {
        List<SchemaMetadata> result = new ArrayList<>();
        Set<String> normalized = new HashSet<>();
        for (String s : schemaNames) {
            if (s != null && !s.trim().isEmpty()) normalized.add(s.trim().toUpperCase());
        }
        if (normalized.isEmpty()) {
            return result;
        }

        DatabaseMetaData meta = oracleConn.getMetaData();
        String catalog = oracleConn.getCatalog();
        if (catalog == null || catalog.isEmpty()) {
            try {
                catalog = oracleConn.getSchema();
            } catch (Throwable ignored) {}
        }

        for (String schemaName : normalized) {
            SchemaMetadata schema = new SchemaMetadata(schemaName);
            readTables(meta, catalog, schemaName, schema);
            result.add(schema);
        }
        return result;
    }

    private void readTables(DatabaseMetaData meta, String catalog, String schemaName, SchemaMetadata schema) throws SQLException {
        try (ResultSet tables = meta.getTables(catalog, schemaName, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (tableName == null) continue;
                String tableType = tables.getString("TABLE_TYPE");
                TableMetadata table = new TableMetadata(schemaName, tableName, tableType != null ? tableType : "TABLE");
                readColumns(meta, catalog, schemaName, tableName, table);
                readPrimaryKeys(meta, catalog, schemaName, tableName, table);
                schema.addTable(table);
            }
        }
    }

    private void readColumns(DatabaseMetaData meta, String catalog, String schemaName, String tableName, TableMetadata table) throws SQLException {
        try (ResultSet cols = meta.getColumns(catalog, schemaName, tableName, "%")) {
            while (cols.next()) {
                String name = cols.getString("COLUMN_NAME");
                if (name == null) continue;
                int dataType = cols.getInt("DATA_TYPE");
                String typeName = cols.getString("TYPE_NAME");
                int columnSize = cols.getInt("COLUMN_SIZE");
                int decimalDigits = cols.getInt("DECIMAL_DIGITS");
                boolean nullable = "YES".equalsIgnoreCase(cols.getString("IS_NULLABLE"));
                String def = cols.getString("COLUMN_DEF");
                String autoIncrement = cols.getString("IS_AUTOINCREMENT");
                boolean isAutoIncrement = "yes".equalsIgnoreCase(autoIncrement);
                table.addColumn(new ColumnMetadata(name, typeName, dataType, columnSize, decimalDigits, nullable, def, isAutoIncrement));
            }
        }
    }

    private void readPrimaryKeys(DatabaseMetaData meta, String catalog, String schemaName, String tableName, TableMetadata table) throws SQLException {
        try (ResultSet pk = meta.getPrimaryKeys(catalog, schemaName, tableName)) {
            while (pk.next()) {
                String colName = pk.getString("COLUMN_NAME");
                if (colName != null) table.addPrimaryKeyColumn(colName);
            }
        }
    }
}
