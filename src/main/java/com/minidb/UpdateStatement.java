package com.minidb;

import java.util.Collections;
import java.util.Map;

// Команда обновления данных: UPDATE
public class UpdateStatement extends Statement {

    private final String tableName;
    private final Map<String, Object> assignments;
    private final WhereCondition where;

    public UpdateStatement(String tableName, Map<String, Object> assignments, WhereCondition where) {
        this.tableName   = tableName;
        this.assignments = Collections.unmodifiableMap(assignments);
        this.where       = where;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, Object> getAssignments() {
        return assignments;
    }

    public WhereCondition getWhere() {
        return where;
    }

    @Override
    public StatementType getType() {
        return StatementType.UPDATE;
    }

    @Override
    public String toString() {
        return "UpdateStatement{tableName='" + tableName
                + "', assignments=" + assignments
                + ", where=" + where + '}';
    }
}
