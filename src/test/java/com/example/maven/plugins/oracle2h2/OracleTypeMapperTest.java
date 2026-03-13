package com.example.maven.plugins.oracle2h2;

import com.example.maven.plugins.oracle2h2.model.ColumnMetadata;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link OracleTypeMapper}.
 */
public class OracleTypeMapperTest {

    private static ColumnMetadata col(String name, String typeName, int dataType, int size, int decimals,
                                      boolean nullable, String defaultValue, boolean autoIncrement) {
        return new ColumnMetadata(name, typeName, dataType, size, decimals, nullable, defaultValue, autoIncrement);
    }

    @Test
    public void varchar2MapsToVarchar2WithSize() {
        assertEquals("VARCHAR2(100)", OracleTypeMapper.toH2Type(col("x", "VARCHAR2", Types.VARCHAR, 100, 0, true, null, false)));
        assertEquals("VARCHAR2(4000)", OracleTypeMapper.toH2Type(col("x", null, Types.VARCHAR, 0, 0, true, null, false)));
    }

    @Test
    public void numberMapsToNumberOrDecimal() {
        assertEquals("NUMBER(10,2)", OracleTypeMapper.toH2Type(col("x", "NUMBER", Types.NUMERIC, 10, 2, true, null, false)));
        assertEquals("NUMBER(10)", OracleTypeMapper.toH2Type(col("x", "NUMBER", Types.NUMERIC, 10, 0, true, null, false)));
        assertEquals("NUMBER", OracleTypeMapper.toH2Type(col("x", "NUMBER", Types.NUMERIC, 0, 0, true, null, false)));
        assertEquals("DECIMAL(8,2)", OracleTypeMapper.toH2Type(col("x", "DECIMAL", Types.DECIMAL, 8, 2, true, null, false)));
    }

    @Test
    public void integerTypesMapToNumber19() {
        assertEquals("NUMBER(19)", OracleTypeMapper.toH2Type(col("id", null, Types.INTEGER, 10, 0, false, null, false)));
        assertEquals("NUMBER(19)", OracleTypeMapper.toH2Type(col("id", null, Types.BIGINT, 19, 0, false, null, false)));
    }

    @Test
    public void dateAndTimestampMapToDateAndTimestamp() {
        assertEquals("DATE", OracleTypeMapper.toH2Type(col("d", null, Types.DATE, 0, 0, true, null, false)));
        assertEquals("TIMESTAMP", OracleTypeMapper.toH2Type(col("ts", null, Types.TIMESTAMP, 0, 0, true, null, false)));
    }

    @Test
    public void clobAndBlobMapToClobAndBlob() {
        assertEquals("CLOB", OracleTypeMapper.toH2Type(col("c", null, Types.CLOB, 0, 0, true, null, false)));
        assertEquals("BLOB", OracleTypeMapper.toH2Type(col("b", null, Types.BLOB, 0, 0, true, null, false)));
    }

    @Test
    public void charMapsToCharWithSize() {
        assertEquals("CHAR(1)", OracleTypeMapper.toH2Type(col("c", null, Types.CHAR, 0, 0, true, null, false)));
        assertEquals("CHAR(10)", OracleTypeMapper.toH2Type(col("c", null, Types.CHAR, 10, 0, true, null, false)));
    }

    @Test
    public void oracleTypeNamesAreHandled() {
        assertEquals("NVARCHAR2(100)", OracleTypeMapper.toH2Type(col("x", "NVARCHAR2", Types.OTHER, 100, 0, true, null, false)));
        assertEquals("RAW(16)", OracleTypeMapper.toH2Type(col("r", "RAW", Types.OTHER, 16, 0, true, null, false)));
        assertEquals("CLOB", OracleTypeMapper.toH2Type(col("l", "LONG", Types.OTHER, 0, 0, true, null, false)));
        assertEquals("TIMESTAMP", OracleTypeMapper.toH2Type(col("t", "TIMESTAMP(6)", Types.OTHER, 0, 0, true, null, false)));
    }
}
