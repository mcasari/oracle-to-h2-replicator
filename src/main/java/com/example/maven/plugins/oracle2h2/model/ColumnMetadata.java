package com.example.maven.plugins.oracle2h2.model;

/**
 * Metadata for a table column extracted from Oracle.
 */
public class ColumnMetadata {
    private final String name;
    private final String typeName;
    private final int dataType;
    private final int columnSize;
    private final int decimalDigits;
    private final boolean nullable;
    private final String defaultValue;
    private final boolean autoIncrement;

    public ColumnMetadata(String name, String typeName, int dataType, int columnSize,
                          int decimalDigits, boolean nullable, String defaultValue, boolean autoIncrement) {
        this.name = name;
        this.typeName = typeName;
        this.dataType = dataType;
        this.columnSize = columnSize;
        this.decimalDigits = decimalDigits;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.autoIncrement = autoIncrement;
    }

    public String getName() { return name; }
    public String getTypeName() { return typeName; }
    public int getDataType() { return dataType; }
    public int getColumnSize() { return columnSize; }
    public int getDecimalDigits() { return decimalDigits; }
    public boolean isNullable() { return nullable; }
    public String getDefaultValue() { return defaultValue; }
    public boolean isAutoIncrement() { return autoIncrement; }
}
