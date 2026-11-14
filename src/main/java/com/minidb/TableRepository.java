package com.minidb;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

// Репозиторий таблицы с сохранением изменений на диск
public class TableRepository implements TableDAO {

    private static final Logger LOG = Logger.getLogger(TableRepository.class.getName());

    private final Table table;
    private final StorageManager storage;

    public TableRepository(Table table, StorageManager storage) {
        if (table == null)   throw new IllegalArgumentException("table must not be null");
        if (storage == null) throw new IllegalArgumentException("storage must not be null");
        this.table   = table;
        this.storage = storage;
    }

    @Override
    public String getTableName() {
        return table.getName();
    }

    @Override
    public Table getTable() {
        return table;
    }

    @Override
    public void insert(Row row) {
        table.insertRow(row);
        flush();
    }

    @Override
    public List<Row> select(Predicate<Row> filter) {
        if (filter == null) return table.getRows();
        return table.selectRows(filter);
    }

    @Override
    public int update(Predicate<Row> filter, Map<String, Object> assignments) {
        Predicate<Row> effective = (filter != null) ? filter : r -> true;
        int count = table.updateRows(effective, assignments);
        if (count > 0) flush();
        return count;
    }

    @Override
    public int delete(Predicate<Row> filter) {
        Predicate<Row> effective = (filter != null) ? filter : r -> true;
        int count = table.deleteRows(effective);
        if (count > 0) flush();
        return count;
    }

    private void flush() {
        try {
            storage.save(table);
        } catch (IOException e) {
            LOG.severe("Failed to persist table '" + table.getName() + "': " + e.getMessage());
        }
    }
}
