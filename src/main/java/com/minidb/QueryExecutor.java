package com.minidb;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Центральный диспетчер выполнения разобранных SQL-инструкций (Statement).
 */
public class QueryExecutor {

    private static final Logger LOG = Logger.getLogger(QueryExecutor.class.getName());

    private final Database      db;
    private final StorageManager storage;
    private final IndexManager  indexManager;

    public QueryExecutor(Database db, StorageManager storage, IndexManager indexManager) {
        this.db           = db;
        this.storage      = storage;
        this.indexManager = indexManager;
    }

    /**
     * Выполняет переданную команду SQL (Statement), маршрутизируя её в соответствующий обработчик.
     * Перехватывает исключения валидации и внутренние ошибки, оборачивая их в QueryResult.
     *
     * @param stmt разобранная SQL-инструкция (DDL или DML)
     * @return результат выполнения запроса (сообщение, количество измененных строк или выборка данных)
     */
    public QueryResult execute(Statement stmt) {
        try {
            switch (stmt.getType()) {
                case CREATE_TABLE:  return executeCreateTable((CreateTableStatement) stmt);
                case DROP_TABLE:    return executeDropTable((DropTableStatement) stmt);
                case INSERT:        return executeInsert((InsertStatement) stmt);
                case SELECT:        return executeSelect((SelectStatement) stmt);
                case UPDATE:        return executeUpdate((UpdateStatement) stmt);
                case DELETE:        return executeDelete((DeleteStatement) stmt);
                case CREATE_INDEX:   return executeCreateIndex((CreateIndexStatement) stmt);
                case SHOW_TABLES:    return executeShowTables();
                case DESCRIBE_TABLE: return executeDescribeTable((DescribeTableStatement) stmt);
                default:
                    return QueryResult.error("Unsupported statement type: " + stmt.getType());
            }
        } catch (IllegalArgumentException e) {
            return QueryResult.error(e.getMessage());
        } catch (Exception e) {
            LOG.severe("Unexpected error: " + e.getMessage());
            return QueryResult.error("Internal error: " + e.getMessage());
        }
    }

    private QueryResult executeCreateTable(CreateTableStatement stmt) throws IOException {
        if (db.hasTable(stmt.getTableName())) {
            return QueryResult.error("Table already exists: " + stmt.getTableName());
        }
        Table table = new Table(stmt.getTableName(), stmt.getColumns());
        db.addTable(table);
        storage.save(table);
        return new QueryResult(0, "Table '" + stmt.getTableName() + "' created.");
    }

    private QueryResult executeDropTable(DropTableStatement stmt) throws IOException {
        String name = stmt.getTableName();
        if (!db.hasTable(name)) {
            return QueryResult.error("Table not found: " + name);
        }
        db.dropTable(name);
        storage.delete(name);
        return new QueryResult(0, "Table '" + name + "' dropped.");
    }

    /**
     * Выполняет вставку строки в таблицу:
     * строит объект Row, проверяет ограничение уникальности первичного ключа и NOT NULL через TableRepository,
     * сохраняет изменения на диск и обновляет связанные индексы таблицы.
     *
     * @param stmt инструкция INSERT с именем таблицы, списком колонок и значениями
     * @return результат операции со счетчиком добавленных строк
     * @throws IOException при ошибках персистентной записи на диск
     */
    private QueryResult executeInsert(InsertStatement stmt) throws IOException {
        Table table = requireTable(stmt.getTableName());
        Row row = buildRow(table, stmt.getColumnNames(), stmt.getValues());

        // Проверка PRIMARY KEY и сохранение
        TableRepository repo = new TableRepository(table, storage);
        repo.insert(row);

        // Обновление индексов для новой строки
        int rowIndex = table.rowCount() - 1;
        indexManager.onInsert(table.getName(), row, rowIndex);

        return new QueryResult(1, "1 row inserted into '" + stmt.getTableName() + "'.");
    }

    /**
     * Формирует объект строки Row из переданных значений и сопоставляет их со схемой таблицы.
     * Приводит типы значений и заполняет неуказанные колонки значениями null.
     *
     * @param table целевая таблица со схемой колонок
     * @param columnNames явный список колонок (или null/пустой для позиционной вставки)
     * @param values значения литералов в порядке следования колонок
     * @return заполненный и приведенный к типам объект Row
     * @throws IllegalArgumentException если количество колонок не совпадает или колонка неизвестна
     */
    private Row buildRow(Table table, List<String> columnNames, List<Object> values) {
        Row row = new Row();

        List<ColumnDefinition> targetCols;
        if (columnNames == null || columnNames.isEmpty()) {
            targetCols = table.getColumns();
        } else {
            targetCols = new ArrayList<>();
            for (String colName : columnNames) {
                ColumnDefinition col = table.getColumn(col(colName));
                if (col == null) {
                    throw new IllegalArgumentException(
                            "Unknown column '" + colName + "' in table '" + table.getName() + "'");
                }
                targetCols.add(col);
            }
        }

        if (targetCols.size() < values.size()) {
            throw new IllegalArgumentException(
                    "Too many values: expected " + targetCols.size() + ", got " + values.size());
        }

        for (int i = 0; i < values.size(); i++) {
            ColumnDefinition targetCol = targetCols.get(i);
            row.set(targetCol.getName(), coerceValue(targetCol, values.get(i)));
        }

        for (int i = values.size(); i < targetCols.size(); i++) {
            row.set(targetCols.get(i).getName(), null);
        }

        for (ColumnDefinition col : table.getColumns()) {
            if (!row.hasColumn(col.getName())) {
                row.set(col.getName(), null);
            }
        }

        return row;
    }

    /**
     * Выполняет запрос выборки SELECT:
     * анализирует наличие индекса по условию WHERE, направляя запрос в indexedSelect при наличии индекса
     * или проводя полное сканирование таблицы (Full Table Scan). Выполняет проекцию на запрошенные колонки.
     *
     * @param stmt инструкция SELECT со списком колонок, таблицей и условием фильтрации
     * @return объект QueryResult с результирующим набором строк и метаданными
     */
    private QueryResult executeSelect(SelectStatement stmt) {
        Table table = requireTable(stmt.getTableName());
        WhereCondition where = stmt.getWhere();

        List<Row> resultRows;

        // Выборка по индексу при его наличии, иначе полное сканирование таблицы
        if (where != null && indexManager.hasIndex(table.getName(), where.getColumnName())) {
            resultRows = indexedSelect(table, where);
        } else {
            Predicate<Row> predicate = (where != null) ? where.toPredicate() : r -> true;
            resultRows = table.selectRows(predicate);
        }

        List<String> selectedCols = resolveColumns(table, stmt.getColumns());
        List<Row> projected = project(resultRows, selectedCols);

        return new QueryResult(selectedCols, projected);
    }

    /**
     * Выполняет оптимизированную выборку строк с использованием индекса таблицы:
     * использует быстрый точечный поиск O(1) для оператора '=' или диапазонный поиск O(log N) в дереве.
     *
     * @param table целевая таблица
     * @param where предикат условия с индексируемой колонкой
     * @return отфильтрованный список строк, извлеченных по номерам позиций
     */
    private List<Row> indexedSelect(Table table, WhereCondition where) {
        Index idx = indexManager.getIndex(table.getName(), where.getColumnName());
        List<Integer> rowIndices;

        Object searchVal = coerceValue(table.getColumn(where.getColumnName()), where.getValue());
        if ("=".equals(where.getOperator())) {
            rowIndices = idx.lookup(searchVal);
        } else {
            rowIndices = idx.range(searchVal, where.getOperator());
        }

        List<Row> allRows = table.getRows();
        List<Row> result  = new ArrayList<>();
        for (int i : rowIndices) {
            if (i < allRows.size()) result.add(allRows.get(i));
        }
        return result;
    }

    private List<String> resolveColumns(Table table, List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            List<String> all = new ArrayList<>();
            for (ColumnDefinition col : table.getColumns()) all.add(col.getName());
            return all;
        }
        for (String col : columns) {
            if (table.getColumn(col(col)) == null) {
                throw new IllegalArgumentException(
                        "Unknown column '" + col + "' in table '" + table.getName() + "'");
            }
        }
        List<String> normalised = new ArrayList<>();
        for (String c : columns) normalised.add(col(c));
        return normalised;
    }

    private List<Row> project(List<Row> rows, List<String> columns) {
        List<Row> result = new ArrayList<>();
        for (Row src : rows) {
            Row projected = new Row();
            for (String col : columns) {
                projected.set(col, src.get(col));
            }
            result.add(projected);
        }
        return result;
    }

    /**
     * Выполняет обновление строк UPDATE:
     * фильтрует целевые строки по предикату WHERE, применяет новые значения с приведением типов,
     * сохраняет изменения в хранилище и перестраивает индексы таблицы.
     *
     * @param stmt инструкция UPDATE с картой присваиваний и предикатом WHERE
     * @return QueryResult с количеством обновленных записей
     */
    private QueryResult executeUpdate(UpdateStatement stmt) {
        Table table = requireTable(stmt.getTableName());
        WhereCondition where = stmt.getWhere();
        Predicate<Row> predicate = (where != null) ? where.toPredicate() : r -> true;

        TableRepository repo = new TableRepository(table, storage);
        Map<String, Object> coercedAssignments = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : stmt.getAssignments().entrySet()) {
            ColumnDefinition colDef = table.getColumn(entry.getKey());
            coercedAssignments.put(entry.getKey(), coerceValue(colDef, entry.getValue()));
        }
        int count = repo.update(predicate, coercedAssignments);

        // Перестроение индексов после обновления
        indexManager.onUpdate(table.getName(), table);

        return new QueryResult(count, count + " row(s) updated in '" + stmt.getTableName() + "'.");
    }

    /**
     * Выполняет приведение значения к целевому типу данных схемы колонки (целые и вещественные числа).
     *
     * @param col определение колонки с типом данных
     * @param raw исходное значение
     * @return приведенное значение
     */
    private Object coerceValue(ColumnDefinition col, Object raw) {
        if (raw == null || col == null) return raw;
        switch (col.getType()) {
            case FLOAT:
                if (raw instanceof Number) return ((Number) raw).doubleValue();
                break;
            case INTEGER:
                if (raw instanceof Number) return ((Number) raw).intValue();
                break;
            default:
                break;
        }
        return raw;
    }

    /**
     * Выполняет удаление строк DELETE:
     * удаляет подходящие под условие WHERE строки, сохраняет таблицу на диск
     * и полностью перестраивает индексы таблицы, так как смещаются позиции оставшихся строк.
     *
     * @param stmt инструкция DELETE с именем таблицы и условием WHERE
     * @return QueryResult с количеством удаленных записей
     */
    private QueryResult executeDelete(DeleteStatement stmt) {
        Table table = requireTable(stmt.getTableName());
        WhereCondition where = stmt.getWhere();
        Predicate<Row> predicate = (where != null) ? where.toPredicate() : r -> true;

        TableRepository repo = new TableRepository(table, storage);
        int count = repo.delete(predicate);

        // Перестроение индексов после удаления строк
        indexManager.onDelete(table.getName(), table);

        return new QueryResult(count, count + " row(s) deleted from '" + stmt.getTableName() + "'.");
    }

    /**
     * Создает и регистрирует новый индекс на таблице:
     * определяет оптимальный тип индекса (TreeIndex для упорядоченных данных, HashIndex для остальных),
     * строит структуру индекса по существующим строкам таблицы и сохраняет дескриптор в indexes.json.
     *
     * @param stmt инструкция CREATE INDEX
     * @return QueryResult с подтверждением создания индекса
     * @throws IOException при ошибках сохранения файла дескрипторов
     */
    private QueryResult executeCreateIndex(CreateIndexStatement stmt) throws IOException {
        Table table = requireTable(stmt.getTableName());
        String col  = stmt.getColumnName();

        if (table.getColumn(col) == null) {
            return QueryResult.error("Column '" + col + "' not found in table '" + stmt.getTableName() + "'");
        }

        // Выбор типа: TREE для упорядоченных колонок (числа, строки, даты), HASH для остальных
        DataType type = table.getColumn(col).getType();
        String indexType = (type == DataType.STRING || type == DataType.INTEGER
                || type == DataType.FLOAT || type == DataType.DATE) ? "TREE" : "HASH";

        Index idx = indexManager.createIndex(
                stmt.getIndexName(), stmt.getTableName(), col, indexType);
        indexManager.rebuildIndex(idx, table);
        indexManager.saveDescriptors();

        return new QueryResult(0,
                "Index '" + stmt.getIndexName() + "' created on "
                + stmt.getTableName() + "(" + col + ") [" + indexType + "].");
    }

    private Table requireTable(String name) {
        Table t = db.getTable(name.toLowerCase());
        if (t == null) {
            Set<String> tables = db.getTableNames();
            String hint = tables.isEmpty()
                    ? " (No tables in database yet. Use CREATE TABLE or SHOW TABLES)"
                    : " (Available tables: " + String.join(", ", tables) + ")";
            throw new IllegalArgumentException("Table not found: '" + name + "'" + hint);
        }
        return t;
    }

    private QueryResult executeShowTables() {
        List<String> columns = Arrays.asList("table_name", "rows", "columns");
        List<Row> rows = new ArrayList<>();
        for (String name : db.getTableNames()) {
            Table t = db.getTable(name);
            Row r = new Row();
            r.set("table_name", t.getName());
            r.set("rows", t.rowCount());
            StringBuilder sb = new StringBuilder();
            for (ColumnDefinition col : t.getColumns()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(col.getName()).append(" (").append(col.getType());
                if (col.isPrimaryKey()) sb.append(" PK");
                sb.append(")");
            }
            r.set("columns", sb.toString());
            rows.add(r);
        }
        return new QueryResult(columns, rows, db.getTableNames().size() + " table(s) in database.");
    }

    private QueryResult executeDescribeTable(DescribeTableStatement stmt) {
        String name = stmt.getTableName();
        Table t = db.getTable(name);
        if (t == null) {
            return QueryResult.error("Table '" + name + "' not found. Available tables: " + db.getTableNames());
        }
        List<String> columns = Arrays.asList("column_name", "data_type", "primary_key", "nullable");
        List<Row> rows = new ArrayList<>();
        for (ColumnDefinition col : t.getColumns()) {
            Row r = new Row();
            r.set("column_name", col.getName());
            r.set("data_type", col.getType().name());
            r.set("primary_key", col.isPrimaryKey());
            r.set("nullable", col.isNullable());
            rows.add(r);
        }
        return new QueryResult(columns, rows, "Schema for table '" + t.getName() + "' (" + t.getColumns().size() + " columns).");
    }

    private String col(String name) {
        return name == null ? null : name.toLowerCase();
    }
}
