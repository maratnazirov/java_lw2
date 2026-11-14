package com.minidb;

// Типы данных колонок в MiniDB
public enum DataType {
    INTEGER,
    FLOAT,
    STRING,
    DATE,
    BOOLEAN;

    public static DataType fromSqlToken(String token) {
        switch (token.toUpperCase()) {
            case "INT":
            case "INTEGER": return INTEGER;
            case "FLOAT":
            case "DOUBLE":
            case "REAL":    return FLOAT;
            case "VARCHAR":
            case "TEXT":
            case "STRING":  return STRING;
            case "DATE":    return DATE;
            case "BOOLEAN":
            case "BOOL":    return BOOLEAN;
            default:
                throw new IllegalArgumentException("Unknown data type: " + token);
        }
    }
}
