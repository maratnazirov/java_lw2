package com.minidb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Центральный реестр для создания, хранения, обновления и восстановления индексов MiniDB.
 */
public class IndexManager {

    private static final String DESCRIPTORS_FILE = "indexes.json";

    private final Map<String, Index> indexes;
    private final Map<String, IndexDescriptor> descriptors;
    private final Path indexDir;
    private final ObjectMapper objectMapper;

    public IndexManager(String indexDirPath) throws IOException {
        this(java.nio.file.Paths.get(indexDirPath));
    }

    public IndexManager(Path indexDir) throws IOException {
        this.indexDir     = indexDir;
        this.indexes      = new HashMap<>();
        this.descriptors  = new HashMap<>();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);

        Files.createDirectories(indexDir);
    }

    /**
     * Создает и регистрирует новый экземпляр индекса (HashIndex или TreeIndex) для указанной таблицы и колонки.
     *
     * @param indexName уникальное логическое имя индекса
     * @param tableName имя таблицы
     * @param columnName индексируемая колонка
     * @param indexType тип структуры данных ("HASH" или "TREE")
     * @return инициализированный экземпляр Index
     */
    public Index createIndex(String indexName, String tableName,
                             String columnName, String indexType) {
        String key   = canonicalKey(tableName, columnName);
        Index  index = buildIndex(tableName, columnName, indexType);

        indexes.put(key, index);

        IndexDescriptor desc = new IndexDescriptor(indexName, tableName,
                                                   columnName, indexType);
        descriptors.put(indexName.toLowerCase(), desc);
        return index;
    }

    public Index getIndex(String tableName, String columnName) {
        return indexes.get(canonicalKey(tableName, columnName));
    }

    public boolean hasIndex(String tableName, String columnName) {
        return indexes.containsKey(canonicalKey(tableName, columnName));
    }

    /**
     * Полностью перестраивает записи индекса, сканируя строки переданной таблицы.
     * Очищает предыдущие данные и сопоставляет каждое значение колонки с его позицией (номером строки).
     *
     * @param index экземпляр индекса для перестроения
     * @param table исходная таблица с актуальными строками
     */
    public void rebuildIndex(Index index, Table table) {
        index.clear();
        List<Row> rows = table.getRows();
        String col = index.getColumnName();
        for (int i = 0; i < rows.size(); i++) {
            Object value = rows.get(i).get(col);
            if (value != null) {
                index.insert(value, i);
            }
        }
    }

    /**
     * Обрабатывает вставку новой строки: извлекает значения индексируемых колонок
     * и добавляет их в соответствующие активные индексы таблицы без полного перестроения.
     *
     * @param tableName имя таблицы, куда вставлена строка
     * @param row добавленная строка данных
     * @param rowIndex позиция (индекс) строки в таблице
     */
    public void onInsert(String tableName, Row row, int rowIndex) {
        for (Map.Entry<String, Index> entry : indexes.entrySet()) {
            Index index = entry.getValue();
            if (!index.getTableName().equalsIgnoreCase(tableName)) continue;
            Object value = row.get(index.getColumnName());
            if (value != null) {
                index.insert(value, rowIndex);
            }
        }
    }

    /**
     * Обрабатывает удаление строк: запускает полное перестроение индексов таблицы,
     * так как при удалении позиции последующих строк сдвигаются.
     *
     * @param tableName имя таблицы
     * @param table таблица после удаления строк
     */
    public void onDelete(String tableName, Table table) {
        rebuildAllForTable(tableName, table);
    }

    /**
     * Обрабатывает обновление строк таблицы: перестраивает все индексы для синхронизации измененных значений.
     *
     * @param tableName имя таблицы
     * @param table таблица в актуальном состоянии
     */
    public void onUpdate(String tableName, Table table) {
        rebuildAllForTable(tableName, table);
    }

    public void saveDescriptors() throws IOException {
        Path file = indexDir.resolve(DESCRIPTORS_FILE);
        List<IndexDescriptor> list = new ArrayList<>(descriptors.values());
        objectMapper.writeValue(file.toFile(), list);
    }

    /**
     * Загружает сохраненные дескрипторы индексов из indexes.json при старте программы
     * и восстанавливает их структуры в оперативной памяти по данным загруженных таблиц.
     *
     * @param tables карта загруженных таблиц (имя таблицы -> Table)
     * @throws IOException при ошибках чтения файла метаданных
     */
    public void loadAndRebuild(Map<String, Table> tables) throws IOException {
        Path file = indexDir.resolve(DESCRIPTORS_FILE);
        if (!Files.exists(file)) return;

        List<IndexDescriptor> loaded = objectMapper.readValue(
                file.toFile(),
                new TypeReference<List<IndexDescriptor>>() {});

        for (IndexDescriptor desc : loaded) {
            String key   = canonicalKey(desc.getTableName(), desc.getColumnName());
            Index  index = buildIndex(desc.getTableName(),
                                      desc.getColumnName(),
                                      desc.getIndexType());
            indexes.put(key, index);
            descriptors.put(desc.getIndexName().toLowerCase(), desc);

            Table table = tables.get(desc.getTableName().toLowerCase());
            if (table != null) {
                rebuildIndex(index, table);
            }
        }
    }

    public void dropIndex(String indexName) throws IOException {
        IndexDescriptor desc = descriptors.remove(indexName.toLowerCase());
        if (desc == null) return;
        indexes.remove(canonicalKey(desc.getTableName(), desc.getColumnName()));
        saveDescriptors();
    }

    public Collection<IndexDescriptor> getAllDescriptors() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    private Index buildIndex(String tableName, String columnName, String indexType) {
        switch (indexType.toUpperCase()) {
            case "HASH": return new HashIndex(tableName, columnName);
            case "TREE": return new TreeIndex(tableName, columnName);
            default:
                throw new IllegalArgumentException(
                        "Unknown index type: " + indexType
                        + ". Expected HASH or TREE.");
        }
    }

    private void rebuildAllForTable(String tableName, Table table) {
        for (Index index : indexes.values()) {
            if (index.getTableName().equalsIgnoreCase(tableName)) {
                rebuildIndex(index, table);
            }
        }
    }

    private String canonicalKey(String tableName, String columnName) {
        return tableName.toLowerCase() + "_" + columnName.toLowerCase();
    }
}
