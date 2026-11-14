package com.minidb;

import java.util.List;

// Интерфейс индекса таблицы базы данных
public interface Index {
    String getTableName();
    String getColumnName();
    void insert(Object key, int rowIndex);
    void delete(Object key);
    void update(Object oldKey, Object newKey, int rowIndex);
    List<Integer> lookup(Object key);
    List<Integer> range(Object value, String operator);
    void clear();
    int size();
}
