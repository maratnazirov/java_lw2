package com.minidb;

import java.io.Serializable;
import java.util.Objects;

// Описание колонки таблицы
public class ColumnDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final DataType type;
    private final boolean primaryKey;
    private final boolean notNull;

    public ColumnDefinition(String name, DataType type, boolean primaryKey, boolean notNull) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name must not be empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Column type must not be null");
        }
        this.name = name.trim();
        this.type = type;
        this.primaryKey = primaryKey;
        this.notNull = primaryKey || notNull;
    }

    public String getName() { return name; }
    public DataType getType() { return type; }
    public boolean isPrimaryKey() { return primaryKey; }
    public boolean isNotNull() { return notNull; }
    public boolean isNullable() { return !notNull; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnDefinition)) return false;
        ColumnDefinition that = (ColumnDefinition) o;
        return primaryKey == that.primaryKey
                && notNull == that.notNull
                && Objects.equals(name, that.name)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, primaryKey, notNull);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name).append(" ").append(type);
        if (primaryKey) sb.append(" PRIMARY KEY");
        if (notNull && !primaryKey) sb.append(" NOT NULL");
        return sb.toString();
    }
}
