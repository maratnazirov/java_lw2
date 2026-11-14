package com.minidb;

import java.util.*;

// Каталог таблиц базы данных в оперативной памяти (Singleton)
public class Database {

    private static final Database INSTANCE = new Database();
    private final Map<String, Table> tables;

    private Database() {
        this.tables = new LinkedHashMap<>();
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public void addTable(Table table) {
        String key = table.getName().toLowerCase();
        if (tables.containsKey(key)) {
            throw new IllegalArgumentException("Table already exists: " + table.getName());
        }
        tables.put(key, table);
    }

    public void putTable(Table table) {
        tables.put(table.getName().toLowerCase(), table);
    }

    public Table dropTable(String tableName) {
        return tables.remove(tableName.toLowerCase());
    }

    public Table getTable(String tableName) {
        return tables.get(tableName.toLowerCase());
    }

    public boolean hasTable(String tableName) {
        return tables.containsKey(tableName.toLowerCase());
    }

    public Map<String, Table> getTables() {
        return Collections.unmodifiableMap(tables);
    }

    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(tables.keySet());
    }

    public void clear() {
        tables.clear();
    }

    @Override
    public String toString() {
        return "Database{tables=" + tables.keySet() + "}";
    }
}
