package com.minidb;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// Интерфейс доступа к данным таблицы (DAO)
public interface TableDAO {
    String getTableName();
    Table getTable();
    void insert(Row row);
    List<Row> select(Predicate<Row> filter);
    int update(Predicate<Row> filter, Map<String, Object> assignments);
    int delete(Predicate<Row> filter);
}
