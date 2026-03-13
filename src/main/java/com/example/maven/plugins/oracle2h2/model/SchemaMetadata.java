package com.example.maven.plugins.oracle2h2.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for an entire schema (tables).
 */
public class SchemaMetadata {
    private final String schemaName;
    private final List<TableMetadata> tables = new ArrayList<>();

    public SchemaMetadata(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getSchemaName() { return schemaName; }
    public List<TableMetadata> getTables() { return tables; }

    public void addTable(TableMetadata table) { tables.add(table); }
}
