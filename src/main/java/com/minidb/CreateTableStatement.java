package com.minidb;

import java.util.Collections;
import java.util.List;

// Команда создания таблицы: CREATE TABLE
public class CreateTableStatement extends Statement {

    private final String tableName;
    private final List<ColumnDefinition> columns;

    public CreateTableStatement(String tableName, List<ColumnDefinition> columns) {
        this.tableName = tableName;
        this.columns   = Collections.unmodifiableList(columns);
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnDefinition> getColumns() {
        return columns;
    }

    @Override
    public StatementType getType() {
        return StatementType.CREATE_TABLE;
    }

    @Override
    public String toString() {
        return "CreateTableStatement{tableName='" + tableName + "', columns=" + columns + '}';
    }
}
