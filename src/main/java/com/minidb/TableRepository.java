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

    /**
     * Вставляет запись в таблицу и синхронизирует изменения с дисковым хранилищем.
     *
     * @param row вставляемая строка
     */
    @Override
    public void insert(Row row) {
        table.insertRow(row);
        flush();
    }

    /**
     * Выбирает строки таблицы, соответствующие предикату фильтрации.
     * Если фильтр равен null, возвращаются все строки таблицы.
     *
     * @param filter условие фильтрации (или null для всех строк)
     * @return список найденных строк
     */
    @Override
    public List<Row> select(Predicate<Row> filter) {
        if (filter == null) return table.getRows();
        return table.selectRows(filter);
    }

    /**
     * Выполняет обновление строк по условию и сохраняет изменения на диск при наличии модификаций.
     *
     * @param filter      условие выбора строк (если null, обновляются все строки)
     * @param assignments сопоставление полей и новых значений
     * @return число фактически обновленных записей
     */
    @Override
    public int update(Predicate<Row> filter, Map<String, Object> assignments) {
        Predicate<Row> effective = (filter != null) ? filter : r -> true;
        int count = table.updateRows(effective, assignments);
        if (count > 0) flush();
        return count;
    }

    /**
     * Удаляет строки по условию и фиксирует изменения в постоянном хранилище.
     *
     * @param filter условие удаления (если null, удаляются все строки)
     * @return число фактически удаленных записей
     */
    @Override
    public int delete(Predicate<Row> filter) {
        Predicate<Row> effective = (filter != null) ? filter : r -> true;
        int count = table.deleteRows(effective);
        if (count > 0) flush();
        return count;
    }

    /**
     * Сохраняет текущее состояние таблицы в файл хранилища (атомарная запись на диск).
     */
    private void flush() {
        try {
            storage.save(table);
        } catch (IOException e) {
            LOG.severe("Failed to persist table '" + table.getName() + "': " + e.getMessage());
        }
    }
}
