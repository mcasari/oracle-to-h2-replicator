package com.example.maven.plugins.oracle2h2;

import com.example.maven.plugins.oracle2h2.model.ColumnMetadata;

import java.sql.Types;

/**
 * Maps Oracle data types to H2-compatible types (Oracle mode).
 * H2 in MODE=Oracle supports VARCHAR2, NUMBER, DATE, TIMESTAMP, CLOB, BLOB, etc.
 */
public final class OracleTypeMapper {

    private OracleTypeMapper() {}

    /**
     * Returns H2 DDL type string for the given column (e.g. "VARCHAR2(100)", "NUMBER(10,2)").
     */
    public static String toH2Type(ColumnMetadata col) {
        String typeName = col.getTypeName() != null ? col.getTypeName().toUpperCase() : "";
        int dataType = col.getDataType();
        int size = col.getColumnSize();
        int decimals = col.getDecimalDigits();

        switch (dataType) {
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
                return varchar2(size);
            case Types.CHAR:
            case Types.NCHAR:
                return "CHAR(" + (size > 0 ? size : 1) + ")";
            case Types.NUMERIC:
            case Types.DECIMAL:
                if ("NUMBER".equals(typeName) || typeName.startsWith("NUMBER")) {
                    if (decimals > 0) return "NUMBER(" + size + "," + decimals + ")";
                    if (size > 0) return "NUMBER(" + size + ")";
                    return "NUMBER";
                }
                if (decimals > 0) return "DECIMAL(" + size + "," + decimals + ")";
                return size > 0 ? "DECIMAL(" + size + ")" : "DECIMAL";
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BIGINT:
                return "NUMBER(19)";
            case Types.FLOAT:
            case Types.REAL:
                return "FLOAT";
            case Types.DOUBLE:
                return "DOUBLE PRECISION";
            case Types.DATE:
                return "DATE";
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return "TIMESTAMP";
            case Types.CLOB:
            case Types.NCLOB:
                return "CLOB";
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return "BLOB";
            case Types.ROWID:
                return "VARCHAR2(18)";
            default:
                if ("VARCHAR2".equals(typeName)) return varchar2(size);
                if ("NVARCHAR2".equals(typeName)) return "NVARCHAR2(" + (size > 0 ? size : 4000) + ")";
                if ("RAW".equals(typeName)) return "RAW(" + (size > 0 ? size : 2000) + ")";
                if ("LONG".equals(typeName)) return "CLOB";
                if ("TIMESTAMP(6)".equals(typeName) || typeName.startsWith("TIMESTAMP")) return "TIMESTAMP";
                if ("INTERVAL".equals(typeName) || typeName.startsWith("INTERVAL")) return "VARCHAR2(50)";
                return "VARCHAR2(" + (size > 0 ? Math.min(size, 4000) : 4000) + ")";
        }
    }

    private static String varchar2(int size) {
        if (size <= 0) return "VARCHAR2(4000)";
        return "VARCHAR2(" + Math.min(size, 4000) + ")";
    }
}
