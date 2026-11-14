package com.minidb;

import java.util.Collections;
import java.util.List;

// Команда вставки данных: INSERT INTO
public class InsertStatement extends Statement {

    private final String tableName;
    private final List<String> columnNames;
    private final List<Object> values;

    public InsertStatement(String tableName, List<String> columnNames, List<Object> values) {
        this.tableName   = tableName;
        this.columnNames = (columnNames == null)
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(columnNames);
        this.values      = Collections.unmodifiableList(values);
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<Object> getValues() {
        return values;
    }

    @Override
    public StatementType getType() {
        return StatementType.INSERT;
    }

    @Override
    public String toString() {
        return "InsertStatement{tableName='" + tableName
                + "', columnNames=" + columnNames
                + ", values=" + values + '}';
    }
}
