package com.minidb;

// Перечисление типов поддерживаемых SQL-команд
public enum StatementType {
    CREATE_TABLE,
    DROP_TABLE,
    INSERT,
    SELECT,
    UPDATE,
    DELETE,
    CREATE_INDEX,
    SHOW_TABLES,
    DESCRIBE_TABLE
}
