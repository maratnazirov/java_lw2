package com.minidb;

// Команда отображения списка таблиц: SHOW TABLES
public class ShowTablesStatement extends Statement {

    @Override
    public StatementType getType() {
        return StatementType.SHOW_TABLES;
    }

    @Override
    public String toString() {
        return "ShowTablesStatement{}";
    }
}
