package com.minidb;

// Команда получения структуры таблицы: DESCRIBE <table>
public class DescribeTableStatement extends Statement {

    private final String tableName;

    public DescribeTableStatement(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public StatementType getType() {
        return StatementType.DESCRIBE_TABLE;
    }

    @Override
    public String toString() {
        return "DescribeTableStatement{tableName='" + tableName + "'}";
    }
}
