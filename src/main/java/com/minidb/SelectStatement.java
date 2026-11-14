package com.minidb;

import java.util.Collections;
import java.util.List;

// Команда выборки данных: SELECT
public class SelectStatement extends Statement {

    private final String tableName;
    private final List<String> columns;
    private final WhereCondition where;

    public SelectStatement(String tableName, List<String> columns, WhereCondition where) {
        this.tableName = tableName;
        this.columns   = Collections.unmodifiableList(columns);
        this.where     = where;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public WhereCondition getWhere() {
        return where;
    }

    @Override
    public StatementType getType() {
        return StatementType.SELECT;
    }

    @Override
    public String toString() {
        return "SelectStatement{tableName='" + tableName
                + "', columns=" + columns
                + ", where=" + where + '}';
    }
}
