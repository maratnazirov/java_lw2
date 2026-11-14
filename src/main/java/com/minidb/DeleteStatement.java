package com.minidb;

// Команда удаления данных: DELETE FROM
public class DeleteStatement extends Statement {

    private final String tableName;
    private final WhereCondition where;

    public DeleteStatement(String tableName, WhereCondition where) {
        this.tableName = tableName;
        this.where     = where;
    }

    public String getTableName() {
        return tableName;
    }

    public WhereCondition getWhere() {
        return where;
    }

    @Override
    public StatementType getType() {
        return StatementType.DELETE;
    }

    @Override
    public String toString() {
        return "DeleteStatement{tableName='" + tableName + "', where=" + where + '}';
    }
}
