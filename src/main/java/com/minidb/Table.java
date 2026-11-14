package com.minidb;

import java.util.*;

// Таблица базы данных со схемой колонок и строками
public class Table {

    private final String name;
    private final List<ColumnDefinition> columns;
    private final List<Row> rows;
    private final int primaryKeyIndex;

    public Table(String name, List<ColumnDefinition> columns) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be empty");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }
        this.name = name.trim().toLowerCase();
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.rows = new ArrayList<>();

        int pkIdx = -1;
        for (int i = 0; i < this.columns.size(); i++) {
            if (this.columns.get(i).isPrimaryKey()) {
                pkIdx = i;
                break;
            }
        }
        this.primaryKeyIndex = pkIdx;
    }

    public String getName() { return name; }

    public List<ColumnDefinition> getColumns() { return columns; }

    public ColumnDefinition getColumn(String columnName) {
        for (ColumnDefinition col : columns) {
            if (col.getName().equalsIgnoreCase(columnName)) return col;
        }
        return null;
    }

    public int getColumnIndex(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(columnName)) return i;
        }
        return -1;
    }

    public ColumnDefinition getPrimaryKeyColumn() {
        return primaryKeyIndex >= 0 ? columns.get(primaryKeyIndex) : null;
    }

    public boolean hasPrimaryKey() { return primaryKeyIndex >= 0; }

    public List<Row> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public int rowCount() { return rows.size(); }

    public void insertRow(Row row) {
        validateRow(row);
        rows.add(row);
    }

    public void addRowUnchecked(Row row) {
        rows.add(row);
    }

    public int deleteRows(java.util.function.Predicate<Row> predicate) {
        int before = rows.size();
        rows.removeIf(predicate);
        return before - rows.size();
    }

    public int updateRows(java.util.function.Predicate<Row> predicate,
                          Map<String, Object> assignments) {
        int count = 0;
        for (Row row : rows) {
            if (predicate.test(row)) {
                for (Map.Entry<String, Object> entry : assignments.entrySet()) {
                    row.set(entry.getKey(), entry.getValue());
                }
                count++;
            }
        }
        return count;
    }

    public List<Row> selectRows(java.util.function.Predicate<Row> predicate) {
        List<Row> result = new ArrayList<>();
        for (Row row : rows) {
            if (predicate.test(row)) result.add(row);
        }
        return result;
    }

    // Валидация ограничений PRIMARY KEY и NOT NULL
    private void validateRow(Row row) {
        if (hasPrimaryKey()) {
            String pkName = getPrimaryKeyColumn().getName();
            Object newPk = row.get(pkName);
            if (newPk == null) {
                throw new IllegalArgumentException(
                        "PRIMARY KEY column '" + pkName + "' cannot be NULL");
            }
            for (Row existing : rows) {
                if (newPk.equals(existing.get(pkName))) {
                    throw new IllegalArgumentException(
                            "Duplicate PRIMARY KEY value: " + newPk);
                }
            }
        }

        for (ColumnDefinition col : columns) {
            if (col.isNotNull() && row.get(col.getName()) == null) {
                throw new IllegalArgumentException(
                        "NOT NULL constraint violated for column: " + col.getName());
            }
        }
    }

    @Override
    public String toString() {
        return "Table{name='" + name + "', columns=" + columns.size() + ", rows=" + rows.size() + "}";
    }
}
