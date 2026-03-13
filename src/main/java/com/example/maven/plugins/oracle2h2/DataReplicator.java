package com.example.maven.plugins.oracle2h2;

import com.example.maven.plugins.oracle2h2.model.ColumnMetadata;
import com.example.maven.plugins.oracle2h2.model.SchemaMetadata;
import com.example.maven.plugins.oracle2h2.model.TableMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Optionally copies table data from Oracle to H2 (structure must already exist in H2).
 */
public class DataReplicator {

    public void copyData(Connection oracleConn, Connection h2Conn, List<SchemaMetadata> schemas) throws SQLException {
        for (SchemaMetadata schema : schemas) {
            String schemaName = schema.getSchemaName();
            for (TableMetadata table : schema.getTables()) {
                copyTable(oracleConn, h2Conn, schemaName, table.getTableName(), table.getColumns());
            }
        }
    }

    private void copyTable(Connection oracleConn, Connection h2Conn, String schema, String tableName,
                           List<ColumnMetadata> columns) throws SQLException {
        if (columns.isEmpty()) return;

        String quotedSchema = quote(schema);
        String quotedTable = quote(tableName);
        String fullTableName = quotedSchema + "." + quotedTable;

        StringBuilder colList = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                colList.append(", ");
                placeholders.append(", ");
            }
            colList.append(quote(columns.get(i).getName()));
            placeholders.append("?");
        }
        String selectSql = "SELECT " + colList + " FROM " + fullTableName;
        String insertSql = "INSERT INTO " + fullTableName + " (" + colList + ") VALUES (" + placeholders + ")";

        try (ResultSet rs = oracleConn.createStatement().executeQuery(selectSql);
             PreparedStatement ps = h2Conn.prepareStatement(insertSql)) {
            int batchSize = 500;
            int count = 0;
            while (rs.next()) {
                for (int i = 1; i <= columns.size(); i++) {
                    ps.setObject(i, rs.getObject(i));
                }
                ps.addBatch();
                count++;
                if (count % batchSize == 0) {
                    ps.executeBatch();
                }
            }
            if (count % batchSize != 0) {
                ps.executeBatch();
            }
        }
    }

    private static String quote(String name) {
        if (name == null) return "\"\"";
        if (name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) return name;
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
