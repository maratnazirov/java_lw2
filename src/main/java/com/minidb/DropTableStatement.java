package com.minidb;

// Команда удаления таблицы: DROP TABLE
public class DropTableStatement extends Statement {

    private final String tableName;

    public DropTableStatement(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public StatementType getType() {
        return StatementType.DROP_TABLE;
    }

    @Override
    public String toString() {
        return "DropTableStatement{tableName='" + tableName + "'}";
    }
}
