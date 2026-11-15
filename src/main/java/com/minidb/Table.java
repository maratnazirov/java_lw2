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

    /**
     * Вставляет новую запись в таблицу с предварительной валидацией ограничений целостности.
     *
     * @param row вставляемая строка данных
     * @throws IllegalArgumentException при нарушении ограничений PRIMARY KEY или NOT NULL
     */
    public void insertRow(Row row) {
        validateRow(row);
        rows.add(row);
    }

    public void addRowUnchecked(Row row) {
        rows.add(row);
    }

    /**
     * Удаляет строки, удовлетворяющие переданному предикату фильтрации.
     *
     * @param predicate условие удаления строки
     * @return количество удаленных строк
     */
    public int deleteRows(java.util.function.Predicate<Row> predicate) {
        int before = rows.size();
        rows.removeIf(predicate);
        return before - rows.size();
    }

    /**
     * Обновляет значения полей в строках, удовлетворяющих заданному предикату.
     *
     * @param predicate   условие выбора строк для обновления
     * @param assignments сопоставление имен обновляемых колонок и их новых значений
     * @return количество обновленных строк
     */
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

    /**
     * Выбирает все строки таблицы, удовлетворяющие условию предиката.
     *
     * @param predicate условие отбора строк
     * @return список отобранных строк
     */
    public List<Row> selectRows(java.util.function.Predicate<Row> predicate) {
        List<Row> result = new ArrayList<>();
        for (Row row : rows) {
            if (predicate.test(row)) result.add(row);
        }
        return result;
    }

    /**
     * Выполняет проверку ограничений схемы таблицы перед вставкой новой записи:
     * 1. Наличие значения в первичном ключе (PRIMARY KEY не может быть NULL).
     * 2. Уникальность первичного ключа среди существующих записей таблицы.
     * 3. Проверка ограничения NOT NULL для обязательных колонок таблицы.
     *
     * @param row проверяемая строка
     * @throws IllegalArgumentException если нарушено любое из ограничений
     */
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
