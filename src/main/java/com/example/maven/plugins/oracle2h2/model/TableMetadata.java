package com.example.maven.plugins.oracle2h2.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for a table extracted from Oracle (columns, primary key).
 */
public class TableMetadata {
    private final String schemaName;
    private final String tableName;
    private final String tableType;
    private final List<ColumnMetadata> columns = new ArrayList<>();
    private final List<String> primaryKeyColumns = new ArrayList<>();

    public TableMetadata(String schemaName, String tableName, String tableType) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.tableType = tableType;
    }

    public String getSchemaName() { return schemaName; }
    public String getTableName() { return tableName; }
    public String getTableType() { return tableType; }
    public List<ColumnMetadata> getColumns() { return columns; }
    public List<String> getPrimaryKeyColumns() { return primaryKeyColumns; }

    public void addColumn(ColumnMetadata col) { columns.add(col); }
    public void addPrimaryKeyColumn(String colName) { primaryKeyColumns.add(colName); }
}
